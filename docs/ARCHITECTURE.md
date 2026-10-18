# GridWatch architecture

This document describes GridWatch with the **C4 model**: four zoom levels, from the big picture to the code.

| Level | Question it answers |
|---|---|
| 1. System context | Who uses GridWatch, and which outside systems does it touch? |
| 2. Containers | Which applications and data stores make up GridWatch, and how do they talk? |
| 3. Components | What is inside each service? |
| 4. Code | What do the main domain classes look like? |

---

## Level 1: System context

```mermaid
flowchart TB
    sensors["Substation sensors<br/><i>send temperature and load readings</i>"]
    operator["Grid operator<br/><i>views work orders and dashboards</i>"]
    tech["Field technician<br/><i>receives maintenance tasks</i>"]

    gw["<b>GridWatch</b><br/>Detects abnormal readings and<br/>creates maintenance work orders"]

    ai["AI model API<br/><i>drafts technician notes</i>"]

    sensors -->|"readings (REST)"| gw
    operator -->|"work orders, dashboards"| gw
    gw -->|"notifications"| tech
    gw -.->|"prompt / note"| ai
```

Sensors push readings into GridWatch. GridWatch decides whether a value is dangerous and, if so, creates a work order and notifies the technicians. Operators look at work orders and dashboards. An external AI model drafts a short note for the technician.

---

## Level 2: Containers

```mermaid
flowchart LR
    sensors["Sensors / clients"]

    subgraph gw["GridWatch (Docker Compose locally, Kubernetes in deployment)"]
        ingest["<b>ingest-service</b><br/>Spring Boot<br/>validate, assign id, publish"]
        kafka[["<b>Kafka</b> (KRaft)<br/>topic gridwatch.readings<br/>key = substationId"]]
        alert["<b>alert-service</b><br/>Spring Boot<br/>dedupe, limit rule, work orders"]
        db[("<b>PostgreSQL</b><br/>readings, work_orders")]
        rabbit[["<b>RabbitMQ</b><br/>queue notifications<br/>+ dead-letter queue"]]
        notifier["<b>notifier-service</b><br/>Spring Boot<br/>notifies technicians"]
        prom["<b>Prometheus</b>"]
        grafana["<b>Grafana</b>"]
    end

    ai["AI model API"]

    sensors -->|"POST /api/readings"| ingest
    ingest -->|"publish reading (JSON)"| kafka
    kafka -->|"consume (group alert-service)"| alert
    alert -->|"JPA"| db
    alert -->|"publish notify task"| rabbit
    rabbit --> notifier
    alert -.->|"circuit breaker"| ai
    prom -.->|"scrape /actuator/prometheus"| ingest
    prom -.-> alert
    prom -.-> notifier
    grafana -.-> prom
```

| Container | Technology | Responsibility |
|---|---|---|
| ingest-service | Java 21, Spring Boot 4 | Public entry point. Validates input, assigns a UUID, publishes to Kafka, answers `202 Accepted` |
| Kafka | Apache Kafka, KRaft | Durable stream of readings; decouples ingest from alert |
| alert-service | Java 21, Spring Boot 4, JPA | Skips duplicates, stores readings, applies limits, creates work orders, requests notifications |
| PostgreSQL | PostgreSQL | Readings and work orders, owned by alert-service only; schema managed with Flyway |
| RabbitMQ | RabbitMQ | Notification tasks that must be done once, with retries and a dead-letter queue |
| notifier-service | Java 21, Spring Boot 4 | Delivers notifications to technicians |
| Prometheus, Grafana | Monitoring | Metrics collection and dashboards |
| AI model API | External | Drafts a technician note, called through a circuit breaker |

---

## Level 3: Components

### ingest-service

```mermaid
flowchart TB
    subgraph ingest["ingest-service"]
        sec["Security config<br/><i>HTTP Basic, CSRF off, stateless,<br/>health open</i>"]
        ctrl["ReadingController<br/><i>POST /api/readings, @Valid,<br/>202 / 400 / 503</i>"]
        svc["ReadingService<br/><i>assign UUID, build event</i>"]
        pub["Reading publisher<br/><i>KafkaTemplate → gridwatch.readings,<br/>key = substationId</i>"]
    end
    ctrl --> svc --> pub
```

### alert-service

```mermaid
flowchart TB
    subgraph alert["alert-service"]
        listener["Reading listener<br/><i>Kafka consumer, group alert-service,<br/>dead-letter topic for bad events</i>"]
        rsvc["ReadingService<br/><i>@Transactional: skip duplicate id,<br/>save reading, compare value with<br/>sensor type limit, open work order</i>"]
        note["Note client<br/><i>AI model API behind a<br/>Resilience4j circuit breaker</i>"]
        notify["Notification publisher<br/><i>RabbitMQ</i>"]
        wo["WorkOrderController<br/><i>GET /work-orders</i>"]
        rrepo["ReadingRepository"]
        wrepo["WorkOrderRepository"]
        db[("PostgreSQL")]
    end
    listener --> rsvc
    rsvc --> rrepo --> db
    rsvc --> wrepo --> db
    rsvc --> note
    rsvc --> notify
    wo --> wrepo
```

### notifier-service

```mermaid
flowchart TB
    subgraph notifier["notifier-service"]
        q["Notification listener<br/><i>RabbitMQ consumer, retries,<br/>dead-letter queue</i>"]
        send["Sender<br/><i>delivers to the technician team</i>"]
    end
    q --> send
```

---

## Level 4: Code (alert-service domain)

```mermaid
classDiagram
    class ReadingEntity {
        UUID id
        String substationId
        SensorTypeOption sensorType
        Double value
        Instant measuredAt
        Instant createdAt
        Instant updatedAt
    }
    class WorkOrderEntity {
        Long id
        String substationId
        String reason
        WorkOrderStatusOption workOrderStatus
        Instant createdAt
        Instant updatedAt
    }
    class SensorTypeOption {
        <<enumeration>>
        TEMPERATURE limit 90
        LOAD limit 100
    }
    class WorkOrderStatusOption {
        <<enumeration>>
        OPEN
        CLOSED
    }
    ReadingEntity --> SensorTypeOption
    WorkOrderEntity --> WorkOrderStatusOption
    ReadingEntity ..> WorkOrderEntity : value above limit creates
```

---

## Reading flow

```mermaid
sequenceDiagram
    participant S as Sensor
    participant I as ingest-service
    participant K as Kafka
    participant A as alert-service
    participant D as PostgreSQL
    participant R as RabbitMQ
    participant N as notifier-service

    S->>I: POST /api/readings
    I->>I: validate, assign UUID
    I->>K: publish (key = substationId)
    I-->>S: 202 Accepted {id}
    K->>A: deliver reading (at least once)
    A->>D: id already stored?
    alt new reading
        A->>D: save reading
        opt value above limit
            A->>D: save work order (OPEN)
            A->>R: notify task
            R->>N: deliver task
        end
    else duplicate
        A->>A: skip
    end
```

---

## Event contract

Topic `gridwatch.readings`, key = `substationId`, value = JSON:

```json
{
  "id": "0b5a3c2e-1d4f-4a6b-9c8d-111111111111",
  "substationId": "DEV-01",
  "sensorType": "TEMPERATURE",
  "value": 95.5,
  "measuredAt": "2026-10-16T09:30:00Z"
}
```

Producer and consumer share this JSON shape, not Java classes, so each service can be deployed on its own.

---

## Deployment

```mermaid
flowchart LR
    dev["Developer push"] --> ci["GitHub Actions<br/><i>build, test (Testcontainers),<br/>Docker images</i>"]
    ci --> reg[("Container registry")]
    reg --> local["Docker Compose<br/><i>local development</i>"]
    reg --> k8s["Kubernetes (Helm)<br/><i>kind locally, AWS</i>"]
```

- **Locally:** one `docker compose up` starts all services, Kafka, RabbitMQ, PostgreSQL, Prometheus and Grafana.
- **Kubernetes:** a Helm chart deploys the services; alert-service runs with two replicas behind a Service for load balancing; health and readiness come from Spring Boot Actuator.

---

## Design decisions

| Decision | Why | Alternative considered |
|---|---|---|
| Kafka between ingest and alert | Readings survive alert-service downtime; new consumers need no change to ingest; replay possible | Synchronous HTTP: simple, but loses data when alert is down |
| Message key = substationId | Readings of one substation stay in order; different substations in parallel | No key: no ordering guarantee |
| UUID per reading, created by ingest | Duplicate detection in alert-service (Kafka delivers at least once) | Database-generated id: only known after saving |
| RabbitMQ for notifications | One-time tasks with per-message retries and a dead-letter queue | Kafka: possible, but a queue fits tasks better |
| Circuit breaker around the AI call | A slow or failing AI service must not stop work orders | Direct call: one outage blocks the flow |
| `Instant` / `timestamptz` | Correct across time zones and daylight saving | `LocalDateTime`: ambiguous |
| Enum stored as text, limit in the enum | Safe when values are added; one place to change limits | Ordinal storage, lookup table |
| Each service owns its data | Independent deployment, clear boundaries | Shared database |
| Flyway migrations, Hibernate validates | Versioned, reviewable schema changes | Hibernate auto-DDL |
| Basic Auth, CSRF off, stateless | Service API with credentials per request | Session login (for browser apps) |
