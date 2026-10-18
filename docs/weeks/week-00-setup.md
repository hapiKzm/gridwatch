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
├── docs/                      architecture, cheat sheets, weekly notes
├── docker-compose.yml         PostgreSQL + Kafka
└── README.md
```

### Target

```
gridwatch/
├── ingest-service/
├── alert-service/
├── notifier-service/          RabbitMQ consumer, technician notifications
├── monitoring/                Prometheus and Grafana configuration
├── helm/                      Kubernetes deployment
├── .github/workflows/         CI: build, test, Docker images
├── docs/
└── docker-compose.yml         full local stack
```

## Running locally

```bash
docker compose up -d          # PostgreSQL (5435) and Kafka (9092)
docker compose ps             # wait until both are (healthy)
```

Then start `AlertServiceApplication` and `IngestServiceApplication` from IntelliJ (in that order). **Always start Docker first**: services create their topics, tables and consumer groups at startup.

| Component | Address |
|---|---|
| ingest-service | http://localhost:8082 |
| alert-service | http://localhost:8081 |
| PostgreSQL | localhost:5435, database `gridwatch` |
| Kafka | localhost:9092 (inside Docker: kafka:29092) |
| Health checks | `/actuator/health` on both services, no login |

Credentials for local development are in each service's `application.yaml`. They are local-only values and will move to environment variables before the repository goes public.

## Conventions

- **Commits:** conventional format `type(scope): description`, e.g. `feat: publish readings to kafka`. Small commits, one step each.
- **Packages:** lowercase.
- **Times:** `Instant` in Java, `timestamp with time zone` in PostgreSQL.
- **Enums:** stored as text.
- **Ids:** UUID for readings (created by ingest-service), `Long` for work orders.
- **Config, not code:** URLs, credentials and topic names live in `application.yaml`.
- **Status codes:** `202` accepted, `400` invalid input, `503` dependency unavailable.
- Log files go to `logs/` (ignored by Git).
