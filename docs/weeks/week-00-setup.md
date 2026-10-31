# Week 0: Setup and project structure

What you need before working on GridWatch, how the repository is organised, and the conventions used.

## Prerequisites

| Tool | Version | Used for |
|---|---|---|
| Java (JDK) | 21 (e.g. Eclipse Temurin) | Building and running the services |
| Maven | via the wrapper (`mvnw`) in each service, no install needed | Build |
| Docker Desktop | recent, engine running | PostgreSQL and Kafka (later the whole stack) |
| IntelliJ IDEA | any recent edition | Development; each service's `pom.xml` must be added as a Maven project |
| Postman (or curl) | any | Sending test requests |
| Git | any | Version control |

**IntelliJ tip:** open the `gridwatch` folder, then right-click `alert-service/pom.xml` and `ingest-service/pom.xml` → *Add as Maven Project*. Set the project SDK to Java 21. Without this, IntelliJ uses wrong defaults (for example "JVM target 5").

## Repository structure

### Now

```
gridwatch/
├── ingest-service/            Spring Boot, port 8082
│   └── src/main/java/com/hapi/gridwatch/ingest/
│       ├── config/            security, Kafka topic
│       ├── controller/        REST endpoint POST /api/readings
│       ├── dto/               incoming request record
│       ├── enums/             valid sensor types
│       ├── rest/              event record published to Kafka
│       └── service/           validation, publish to Kafka
├── alert-service/             Spring Boot, port 8081
│   └── src/main/java/com/hapi/gridwatch/alert/
│       ├── config/            security
│       ├── controller/        REST endpoints
│       ├── dto/               event and response records
│       ├── enums/             sensor types with limits, work order status
│       ├── exception/         domain exceptions
│       ├── kafka/             listener, error handler + DLT, topics
│       ├── model/             JPA entities
│       ├── repository/        Spring Data repositories
│       └── service/           duplicate check, limit rule, work orders
├── notifier-service/          Spring Boot, port 8083
│   └── src/main/java/com/hapi/gridwatch/notifier/
│       ├── dto/               event record
│       ├── mail/              email sending
│       ├── model/             sent_notification entity
│       ├── rabbit/            queue, binding, DLQ, retry rule, listener
│       ├── repository/        Spring Data repository
│       └── service/           send once per work order
├── docs/                      architecture, cheat sheets, weekly notes
├── docker-compose.yml         PostgreSQL, Kafka, RabbitMQ, Mailpit, all services
└── README.md
```

### Target

```
gridwatch/
├── ingest-service/
├── alert-service/
├── notifier-service/
├── monitoring/                Prometheus and Grafana configuration
├── helm/                      Kubernetes deployment
├── .github/workflows/         CI: build, test, Docker images
├── docs/
└── docker-compose.yml         full local stack
```

## Running locally

**Everything in Docker (default):**

```bash
cp .env.example .env          # once, then fill in your own passwords
docker compose up -d --build  # all services, brokers, mailpit, prometheus, grafana
docker compose --profile simulator up -d --build  # optional: also send readings automatically
docker compose ps             # db and kafka (healthy), services Up
```

**Services from IntelliJ (while developing):**

```bash
docker compose up -d db kafka rabbitmq mailpit # only the infrastructure
docker compose ps             # wait until all are (healthy)
```

Then start `AlertServiceApplication`, `IngestServiceApplication` and `NotifierServiceApplication` from IntelliJ. Stop the `alert`, `ingest` and `notifier` containers first, they use the same ports. **Always start Docker first**: services create their topics, tables and consumer groups at startup.

| Component | Address |
|---|---|
| ingest-service | http://localhost:8082 |
| alert-service | http://localhost:8081 |
| notifier-service | port 8083 (no public API) |
| RabbitMQ | localhost:5672 (inside Docker: rabbitmq:5672), dashboard http://localhost:15672 |
| Mailpit | SMTP localhost:1025 (inside Docker: mailpit:1025), inbox http://localhost:8025 |
| PostgreSQL | localhost:5435, databases `gridwatch` (alert) and `notifier` |
| Kafka | localhost:9092 (inside Docker: kafka:29092) |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000, user `admin`, password from `.env` |
| Health checks | `/actuator/health` on every service, no login |
| Metrics | `/actuator/prometheus` on every service, no login |

Passwords come from environment variables. Docker Compose reads them from `.env` (copy `.env.example`; never commit `.env`). From IntelliJ and in tests, the local defaults in each service's `application.yaml` are used.

## Conventions

- **Commits:** conventional format `type(scope): description`, e.g. `feat: publish readings to kafka`. Small commits, one step each.
- **Packages:** lowercase.
- **Times:** `Instant` in Java, `timestamp with time zone` in PostgreSQL.
- **Enums:** stored as text.
- **Ids:** UUID for readings (created by ingest-service), `Long` for work orders.
- **Config, not code:** URLs, credentials and topic names live in `application.yaml`.
- **Status codes:** `202` accepted, `400` invalid input, `503` dependency unavailable.
- Log files go to `logs/` (ignored by Git).
