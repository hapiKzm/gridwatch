# Kafka cheat sheet (GridWatch)

The short version: the concepts you need, the settings GridWatch uses and why, the commands, and the traps we hit.

## Concepts in one table

| Term | Meaning | In GridWatch |
|---|---|---|
| Topic | Named stream of events | `gridwatch.readings`, `gridwatch.readings-dlt` |
| Partition | Ordered log inside a topic; unit of parallelism and ordering | 3 per topic |
| Key | Decides the partition; same key → same partition → in order | `substationId` |
| Offset | Position of a message in a partition | — |
| Producer | Writes events | ingest-service |
| Consumer group | Consumers sharing the partitions; each group gets every event | `alert.gridwatch` |
| Committed offset | Where a group continues after a restart | committed by Spring after the listener succeeds |
| Lag | End offset − committed offset; how far behind a group is | should be 0 |
| DLT | Dead-letter topic for messages that can't be processed | `gridwatch.readings-dlt` |
| KRaft | Kafka manages itself, no ZooKeeper | one container is broker + controller |

**Delivery:** Kafka is *at least once* by default → consumers must be **idempotent**. GridWatch: every reading has a UUID; alert-service skips ids it already stored.

## Settings GridWatch uses

### Docker Compose (`kafka` service)

| Setting | Why |
|---|---|
| `apache/kafka:4.1.0`, `KAFKA_PROCESS_ROLES: broker,controller` | One KRaft node, no ZooKeeper |
| `KAFKA_LISTENERS` / `KAFKA_ADVERTISED_LISTENERS` | Two listeners: `localhost:9092` for apps on the laptop, `kafka:29092` for containers |
| `*_REPLICATION_FACTOR: 1` | Only one broker (defaults expect 3) |
| `KAFKA_LOG_DIRS` + volume `kafka:/var/lib/kafka/data` | Events survive `docker compose down` |
| Healthcheck `kafka-broker-api-versions.sh --bootstrap-server localhost:9092` | Shows when Kafka is really ready |

### ingest-service (producer)

| Property | Value | Why |
|---|---|---|
| `bootstrap-servers` | `localhost:9092` | `kafka:29092` when running inside Docker |
| `key-serializer` | `StringSerializer` | Key = substationId |
| `value-serializer` | `JacksonJsonSerializer` | Value = reading as JSON |
| `acks` | `all` | Confirm only when safely stored |
| `enable.idempotence` | `true` | No duplicates from producer retries |
| `spring.json.add.type.headers` | `false` | Don't send the Java class name; services share only the JSON shape |
| `max.block.ms` | `5000` | Don't hang 60 s when Kafka is down |
| `request.timeout.ms` / `delivery.timeout.ms` | `3000` / `5000` | Fail fast; delivery ≥ request + linger |

In code: `send(...).get(5, SECONDS)` → `202` only after Kafka confirmed, otherwise `503`.

### alert-service (consumer + DLT producer)

| Property | Value | Why |
|---|---|---|
| `group-id` | `alert.gridwatch` | One group, all partitions |
| `auto-offset-reset` | `earliest` | A new group starts from the oldest message |
| `value-deserializer` | `ErrorHandlingDeserializer` | Broken JSON doesn't crash the consumer; it becomes an error the handler can route to the DLT |
| `spring.deserializer.value.delegate.class` | `JacksonJsonDeserializer` | The real JSON deserializer |
| `spring.json.value.default.type` | `...alert.dto.ReadingRestRequest` | Which class to build |
| `spring.json.trusted.packages` | `com.hapi.gridwatch.alert.dto` | Security: only these classes may be created |
| `spring.json.use.type.headers` | `false` | Ignore any `__TypeId__` header from the producer |
| producer settings | as above | Needed to publish to the DLT |

In code: `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`, `FixedBackOff(1000, 3)`, `UnknownSensorTypeException` not retried, DLT template with `DelegatingByTypeSerializer` (`byte[]` → bytes, objects → JSON). DLT has the same 3 partitions as the source topic.

## Wiring it up: code per part

### 1. Docker: one Kafka broker (`docker-compose.yml`)

```yaml
kafka:
  image: apache/kafka:4.1.0
  ports:
    - "9092:9092"
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_LISTENERS: PLAINTEXT://:29092,PLAINTEXT_HOST://:9092,CONTROLLER://:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    KAFKA_LOG_DIRS: /var/lib/kafka/data
  volumes:
    - kafka:/var/lib/kafka/data
  healthcheck:
    test: ["CMD-SHELL", "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1"]
    interval: 10s
    retries: 10
```

Dependency in both `pom.xml`: `org.springframework.kafka:spring-kafka` (version from Spring Boot).

### 2. Create a topic (both services)

```java
@Configuration
public class Topics {
    @Bean
    public NewTopic readingTopic(@Value("${kafka-topic.reading}") String topic) {
        return new NewTopic(topic, 3, (short) 1);   // name, partitions, replicas
    }
}
```

```yaml
kafka-topic:
  reading: gridwatch.readings          # ingest
  reading-dlt: gridwatch.readings-dlt  # alert
```

### 3. Producer (ingest-service)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JacksonJsonSerializer
      acks: all
      properties:
        enable.idempotence: true
        spring.json.add.type.headers: false
        max.block.ms: 5000
        request.timeout.ms: 3000
        delivery.timeout.ms: 5000
```

```java
@Service
public class ReadingService {
    private final KafkaTemplate<String, ReadingRestRequest> kafka;   // auto-configured by Spring Boot
    private final String topic;

    public ReadingService(KafkaTemplate<String, ReadingRestRequest> kafka,
                          @Value("${kafka-topic.reading}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public UUID publish(UserReadingRequest in) {
        UUID id = UUID.randomUUID();
        var out = new ReadingRestRequest(id, in.substationId(), in.sensorType(), in.value(), in.measuredAt());
        try {
            var result = kafka.send(topic, in.substationId(), out).get(5, TimeUnit.SECONDS); // key = substationId
            log.info("Published {} to partition {} offset {}", id,
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            return id;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaUnavailableException("Interrupted while publishing", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new KafkaUnavailableException("Kafka not reachable", e);   // → 503
        }
    }
}
```

### 4. Consumer (alert-service)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: alert.gridwatch
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JacksonJsonDeserializer
        spring.json.value.default.type: com.hapi.gridwatch.alert.dto.ReadingRestRequest
        spring.json.trusted.packages: com.hapi.gridwatch.alert.dto
        spring.json.use.type.headers: false
```

```java
@Service
public class Listener {
    private final ReadingService readingService;

    public Listener(ReadingService readingService) {
        this.readingService = readingService;
    }

    @KafkaListener(topics = "gridwatch.readings", groupId = "alert.gridwatch")
    public void onReading(ReadingRestRequest reading, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        readingService.saveReading(reading);   // exception → error handler; success → offset committed
    }
}
```

### 5. Retries + dead-letter topic (alert-service)

alert-service also needs the **producer** block from part 3 (it publishes to the DLT). Spring Boot automatically attaches a `DefaultErrorHandler` bean to the listener.

```java
@Configuration
public class KafkaErrorHandler {

    @Bean
    KafkaTemplate<Object, Object> dltTemplate(ProducerFactory<Object, Object> producerFactory) {
        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());     // couldn't deserialize: original bytes
        delegates.put(Object.class, new JacksonJsonSerializer<>()); // failed in processing: JSON
        var valueSerializer = new DelegatingByTypeSerializer(delegates, true);
        var factory = new DefaultKafkaProducerFactory<Object, Object>(
                producerFactory.getConfigurationProperties(), null, valueSerializer);
        return new KafkaTemplate<>(factory);
    }

    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template);          // → <topic>-dlt, same partition
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(UnknownSensorTypeException.class);  // straight to the DLT
        return handler;
    }
}
```

```java
@Bean
public NewTopic readingDLT(@Value("${kafka-topic.reading-dlt}") String topic) {
    return new NewTopic(topic, 3, (short) 1);   // same partition count as the source topic
}
```

### 6. Running inside Docker (Part 6)

Same yaml, overridden by environment variables (Spring maps `SPRING_KAFKA_BOOTSTRAP_SERVERS` → `spring.kafka.bootstrap-servers`):

```yaml
environment:
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
  SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/gridwatch
depends_on:
  kafka:
    condition: service_healthy
```

## Commands

Run from the `gridwatch` folder.

```bash
docker compose up -d                 # start db + kafka
docker compose ps                    # wait for (healthy), then start the services
docker compose logs kafka --tail 50
docker compose down                  # keep data;  down -v deletes data
docker compose exec kafka bash       # shell inside Kafka; then: cd /opt/kafka/bin
```

Inside the container (`/opt/kafka/bin`, always `--bootstrap-server localhost:9092`):

```bash
# topics
./kafka-topics.sh --bootstrap-server localhost:9092 --list
./kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic gridwatch.readings
./kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic <name>

# read (own random group, doesn't move app offsets)
./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic gridwatch.readings --from-beginning \
  --property print.key=true --property print.partition=true --property print.headers=true --timeout-ms 5000

# write by hand, key:value per line
./kafka-console-producer.sh --bootstrap-server localhost:9092 --topic gridwatch.readings \
  --property parse.key=true --property key.separator=:

# group, offsets, lag
./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group alert.gridwatch

# replay (stop alert-service first; --dry-run to preview)
./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group alert.gridwatch \
  --topic gridwatch.readings --reset-offsets --to-earliest --execute
```

Database: `docker compose exec db psql -U gridwatch -d gridwatch`, then `\dt`, `select * from reading;`, `\q`.

## Self-check after sending a reading

1. `docker compose ps` → db and kafka healthy
2. `POST :8082/api/readings` → `202`
3. Console consumer → message in `gridwatch.readings` with key = substationId
4. Group describe → lag 0
5. `select ... from reading` → row saved (+ work order if above the limit)
6. 4 fine but 5 empty → look in alert-service's log for the last `Caused by`

## Traps we hit

| Symptom | Cause | Fix |
|---|---|---|
| Topic doesn't exist, no consumer group | Services started before Kafka (or Kafka recreated) | Start Docker first, wait for healthy, then the services |
| Healthcheck always unhealthy | `--bootstrap-serverlocalhost` (missing space) | `--bootstrap-server localhost:9092` |
| "class ... ingest ... not in the trusted packages" | Producer sent a `__TypeId__` header with its own class | `add.type.headers=false` (producer), `use.type.headers=false` + `default.type` (consumer) |
| Message consumed (lag 0) but nothing saved | Default handler retried, logged and skipped it | Error handler + DLT |
| DLT stays empty, lag grows, good messages stuck | DLT publish failing → record never recovered → partition blocked | Fix the DLT producer (here: JSON serializer configured both in code and properties) |
| DLT with 1 partition | Auto-created before the `NewTopic` bean | Delete it; let the bean create it with 3 partitions |
| `send()` hangs 60 s when Kafka is down | `max.block.ms` default | Set it to 5000 |
| `@Value` field is null | `@Value` on a static field | Inject through a constructor or `@Bean` parameter |

Full tutorial (concepts, advanced topics, 3-broker cluster lab, interview questions): kept outside the repo as `Kafka_Tutorial_Full.pdf`.
