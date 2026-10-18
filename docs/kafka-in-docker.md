# Kafka and the services in Docker

Where Docker and Kafka meet: how Kafka runs as a container, and how the services reach it and the database. Read [docker-basics.md](docker-basics.md) and [kafka-basics.md](kafka-basics.md) first.

## The address rule

| Who calls | Database | Kafka |
|---|---|---|
| App in IntelliJ on the laptop | `localhost:5435` | `localhost:9092` |
| App inside Docker | `db:5432` | `kafka:29092` |

Inside a container, `localhost` means *that container itself*. Containers in the same Compose file reach each other by service name, on the container's own port.

## Why Kafka has two addresses (listeners)

A client first asks Kafka "where are you?" and then connects to the address Kafka *answers* with (the advertised listener). One answer can't be right for both the laptop and other containers, so Kafka gets two doors:

```yaml
KAFKA_LISTENERS: PLAINTEXT://:29092,PLAINTEXT_HOST://:9092,CONTROLLER://:9093
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
```

| Door | Listens on | Tells clients | Used by |
|---|---|---|---|
| `PLAINTEXT` | 29092 | `kafka:29092` | containers (alert, ingest) |
| `PLAINTEXT_HOST` | 9092 | `localhost:9092` | apps in IntelliJ, published with `ports: "9092:9092"` |
| `CONTROLLER` | 9093 | — | Kafka itself (KRaft) |

Wrong door = the first connection works, then the client tries an address it can't reach and keeps retrying.

## Same yaml, different address

`application.yaml` keeps the laptop values. Compose overrides them with environment variables (uppercase, dots and dashes become `_`):

```yaml
alert:
  build: ./alert-service
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/gridwatch
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
  depends_on:
    db: { condition: service_healthy }
    kafka: { condition: service_healthy }
ingest:
  build: ./ingest-service
  environment:
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
  depends_on:
    kafka: { condition: service_healthy }
```

## Start order

The services create topics and consumer groups at startup, so Kafka must be ready first. `depends_on` with `condition: service_healthy` waits for Kafka's healthcheck (`kafka-broker-api-versions.sh`), not just for the container to exist.

Running in IntelliJ instead: start `docker compose up -d db kafka` first, wait for `(healthy)`, then the services. Never run the same service in IntelliJ and Docker at the same time: they fight over the port.

## Data that survives

| Volume | Holds | Gone after |
|---|---|---|
| `postgres-data` | readings, work orders | `docker compose down -v` |
| `kafka` | topics, events, committed offsets | `docker compose down -v` |

After `down -v`, Kafka is empty: topics are recreated by the services on the next start.

## Checks

```bash
docker compose ps                                   # all healthy / up
docker compose logs alert --tail 30                 # "partitions assigned"
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group alert.gridwatch
```

Inside the Kafka container, `localhost:9092` is correct: the command runs in the Kafka box itself.
