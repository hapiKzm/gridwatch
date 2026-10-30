# Week 4: Flyway and tests

## Goal

Database tables are created by versioned SQL scripts instead of Hibernate, and the important behaviour is protected by tests: fast unit tests for the business rules, and integration tests that run each service against real PostgreSQL, Kafka and RabbitMQ in Docker.

## What was built

### Flyway
- `spring-boot-starter-flyway` + `flyway-database-postgresql` in alert and notifier.
- `db/migration/V1__init.sql` per service: alert creates `reading` and `work_order`, notifier creates `sent_notification`.
- Hibernate only checks the tables (`ddl-auto: validate`), it no longer changes them.
- `docker/postgres/init.sql` creates the `notifier` database when the PostgreSQL volume is new.

### Unit tests (JUnit 5 + Mockito)

| Class | Test | Checks |
|---|---|---|
| alert `ReadingServiceTest` | `temperatureAboveLimitCreatesWorkOrder` | reading saved, work order saved, event published |
| | `valueAtLimitCreatesNoWorkOrder` | reading saved, no work order, no event |
| | `duplicateReadingIsSkipped` | nothing saved again |
| | `unknownSensorTypeIsRejected` | `InvalidReadingException`, nothing saved |
| | `missingValueIsRejected` | `InvalidReadingException`, nothing saved |
| notifier `NotificationServiceTest` | `newWorkOrderIsSavedAndEmailed` | id saved, email sent for this event |
| | `alreadyNotifiedSendsNoEmail` | no save, no email |
| | `emailFailureThrows` | the mail exception reaches RabbitMQ (so it retries) |

### Integration tests (Testcontainers)

| Class | Containers | Test | Checks |
|---|---|---|---|
| alert `AlertFlowIT` | PostgreSQL, Kafka, RabbitMQ | `readingAboveLimitCreatesWorkOrderAndPublishes` | reading on Kafka → reading and work order in DB → JSON message on the exchange |
| | | `readingBelowLimitCreatesNoWorkOrder` | reading saved, no work order, no message |
| | | `duplicateReadingIsSavedOnce` | same reading sent twice → 1 reading, 1 work order |
| notifier `NotificationFlowIT` | PostgreSQL, RabbitMQ | `messageIsSavedAndEmailed` | message on the exchange → row in `sent_notification`, email sent |
| | | `failedEmailRollsBackSave` | mail sender throws → 3 attempts, no row (rollback) |

How the integration tests work:

- `@SpringBootTest` starts the whole service, with its real listeners.
- `@Container` + `@ServiceConnection` on `static` fields start the containers once per class and point Spring at them; no URLs or passwords in the test.
- The test plays the neighbouring services: in alert it sends readings to Kafka (fake ingest) and binds its own queue `test.work-orders` with `#` (fake notifier); in notifier it publishes work orders to the exchange (fake alert).
- `@MockitoBean JavaMailSender` replaces only the mail sender, so a mail failure can be forced.
- Listeners run on other threads, so tests wait with Awaitility before checking.
- `@BeforeEach` cleans the tables and purges the test queue, so every test starts empty.

### Build

- `maven-failsafe-plugin` in alert and notifier: classes ending in `Test` run in `mvn test`, classes ending in `IT` run in `mvn verify`.
- The empty `*ApplicationTests` context tests were removed: they needed services on `localhost`, and the `IT` classes already start the whole application.

## How to run

```bash
alert-service/mvnw -f alert-service/pom.xml clean test      # unit tests, seconds, no Docker
alert-service/mvnw -f alert-service/pom.xml clean verify    # unit + integration, ~1 min, Docker running
notifier-service/mvnw -f notifier-service/pom.xml clean verify
```

Results: ingest 8 unit and web + 1 integration, alert 5 unit + 3 integration, notifier 3 unit + 2 integration, all passing. Explained step by step in [testing-cheatsheet.md](../testing-cheatsheet.md).

## Design decisions

| Decision | Reason | Alternative |
|---|---|---|
| Flyway instead of `ddl-auto: update` | Schema changes are versioned, reviewed and repeatable; Hibernate never drops or guesses | Let Hibernate change tables |
| No `baseline-on-migrate` | It marked V1 as done on an existing database, so the script itself was never tested | Baseline existing databases |
| Unit tests for rules, integration tests for wiring | Rules are fast to test with fakes; transactions, serialization and broker config only fail with the real thing | Only one kind |
| Testcontainers instead of the Compose stack | Each run starts clean, same versions as production, no shared state | Tests against `localhost` services |
| Mock only the mail sender in notifier | Lets the test force a failure; everything else stays real | Mailpit container |
| Read the raw RabbitMQ message in alert's test | Checks the JSON contract notifier depends on, not a Java class | `receiveAndConvert` with trusted packages |
| `verify` only the effects (save, email, publish) | Tests don't break when the lookup changes | Verify every call |

## Problems solved along the way

| Problem | Cause | Fix |
|---|---|---|
| `relation "work_order_id_seq" already exists` on a fresh database | Identity column defined twice in V1 | Removed the extra `ALTER ... IDENTITY` block |
| `NoClassDefFoundError` at test discovery | Stale `target/` folder | `mvn clean test` |
| Copy-paste in unit tests checked the wrong repository | `verify(workOrderRepository, never())` twice | Check `readingRepository` too |
| Test file not found by Maven | Notifier's test placed under alert-service | Moved to `notifier-service/src/test/...` |
| `'beans' must not be empty` (mail health) | Mocked `JavaMailSender` is not a real mail sender | `management.health.mail.enabled=false` in the test |
| `mvn test` skipped the IT classes | Surefire only runs `*Test`/`*Tests` | Failsafe plugin, `mvn verify` |
| `integration-test` goal not found | Failsafe executions pasted into the Spring Boot plugin | Separate `<plugin>` block |
| `@Autowired` fields `null` | Fields were `static` (same trap as `@Value static` in week 3) | Only `@Container` fields are static |
| Setup method not run | `private` / `@BeforeAll` before Spring injects | `@BeforeEach` on a package-private method |
| No `KafkaTemplate<String, Object>` bean | alert only has the dead-letter template `<Object, Object>` | Use that type |
| Count checked before the reading arrived | Assertion before `await` | Wait first, then check |
| Class "not in the trusted packages" | `receiveAndConvert` builds the class named in the header | `receive` and check the JSON body |
| `is(null)` crashed | `is` expects a condition object | `isNull()` |
| Duplicate test could pass too early | Wait ended after the first copy | `pollDelay(3 s)` before checking counts |
| `Can't get Docker image` | Docker Desktop not running | Start Docker first |
| `-Dit.test=...` "unknown lifecycle phase" | PowerShell splits arguments at the dot | Quote `-D` arguments in PowerShell |

## Commits

`feat: manage schemas with flyway and create notifier database on startup` → `test: add unit tests for reading and notification services` → `docs: add diagrams for overview, kafka, rabbitmq and docker` → `test: add testcontainers integration tests for notifier` → `test: add testcontainers integration tests for alert`

More: [messaging-flow.md](../messaging-flow.md), [week-03-rabbitmq.md](week-03-rabbitmq.md).
