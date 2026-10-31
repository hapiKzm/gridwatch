# Self-test before week 6

Weeks 1-5 went fast. This file checks that you can do it and explain it without help.

**How to use it:** Part 1 is real features in this project: predict first, then build, no looking at old code unless stuck. Part 2 is questions: answer out loud or on paper. Part 3 is the answers: read them only after trying.

## Part 1: Exercises

Tick these off in [PLAN.md](PLAN.md). Small commits, one per exercise.

### 1. Flyway: add a column

- **Goal:** change a table safely.
- **Build:** a V2 migration in alert that adds `closed_at` to `work_order`, plus the entity field.
- **Predict first:** what happens if you add only the entity field? What if you edit V1 instead?
- **Done when:** a fresh `docker compose down -v` start works, `flyway_schema_history` shows version 2, and V1 is unchanged.
- **Review:** [week-04-testing.md](weeks/week-04-testing.md)

### 2. RabbitMQ: a second event

- **Goal:** add an event without changing the routing.
- **Build:** an alert endpoint that closes a work order, sets `closed_at`, and publishes `work-order.closed` after commit. notifier emails it.
- **Predict first:** does the notifier queue or binding need to change? Look closely at what `sent_notification` uses as primary key: what happens to the closed event for a work order that was already notified?
- **Done when:** closing sends one email, and sending the same closed event twice still sends one.
- **Review:** [rabbitmq-basics.md](rabbitmq-basics.md), [rabbitmq-cheatsheet.md](rabbitmq-cheatsheet.md)

### 3. RabbitMQ: a second consumer

- **Goal:** see one message reach two queues.
- **Build:** a queue `audit.work-orders` bound to the same exchange, with a listener that logs each event.
- **Predict first:** does alert change? What is the difference between this and two listeners on one queue?
- **Done when:** one work order produces a message in both queues.
- **Review:** [messaging-flow.md](messaging-flow.md)

### 4. Kafka: groups and instances

- **Goal:** see consumer groups and rebalancing happen.
- **Build:** first, a second `@KafkaListener` with a different group that only logs. Then run 2 alert instances in one group.
- **Predict first:** does the new group get old readings? How are 3 partitions split between 2 instances? What with 4 instances?
- **Done when:** the new group read from the start, and the logs show partitions being reassigned when you stop one instance.
- **Review:** [kafka-basics.md](kafka-basics.md), [kafka-cheatsheet.md](kafka-cheatsheet.md)

### 5. Kafka: lag

- **Goal:** read offsets and lag yourself.
- **Build:** nothing. Stop alert, run the simulator for a minute, watch lag with `kafka-consumer-groups.sh --describe`, restart alert.
- **Predict first:** what happens to the numbers in CURRENT-OFFSET, LOG-END-OFFSET and LAG?
- **Done when:** lag rose, went back to 0, and the reading count matches.
- **Review:** [kafka-in-docker.md](kafka-in-docker.md)

### 6. Testing: the failure paths

- **Goal:** test what happens when things go wrong.
- **Build:** a unit test for closing a work order; an IT proving an invalid message reaches `notifier.work-orders.dlq` with no retries; an IT proving a bad reading reaches `gridwatch.readings-dlt`.
- **Predict first:** how will the test tell "no retries" from "3 retries"? How will it read from the dead-letter topic?
- **Done when:** `verify` is green for all services, and each new test fails when you break the code on purpose. Then undo the break.
- **Review:** [testing-cheatsheet.md](testing-cheatsheet.md)

### 7. Grafana: see failures

- **Goal:** the dashboard should show trouble, not only traffic.
- **Build:** a panel for messages that reach dead-letter, a panel for RabbitMQ queue depth, and one Grafana alert rule on error rate.
- **Hint:** RabbitMQ has a built-in Prometheus plugin. Find its port and add a target. For dead letters, what in your code can count them?
- **Predict first:** with notifier stopped and the simulator on, what does queue depth do?
- **Done when:** stopping Kafka fires the alert, and the panels are saved in `gridwatch.json`.
- **Review:** [monitoring-cheatsheet.md](monitoring-cheatsheet.md)

### 8. The whole system

- **Goal:** hold the whole design in your head.
- **Do:** on paper, draw the full flow from memory, with every port, topic, queue and key. Compare with [01-overview.svg](images/01-overview.svg). Then choose 3 things to break (for example remove the duplicate check, stop PostgreSQL, wrong routing key), write down your prediction, and try each.
- **Done when:** you can explain every difference between prediction and result.

## Part 2: Questions

### Kafka

1. What is the difference between a topic and a partition?
2. Why is the key `substationId`?
3. What is an offset, and who stores it?
4. What happens if 2 consumers in one group read a topic with 3 partitions? With 5 consumers?
5. What happens when a consumer dies?
6. What does at-least-once mean, and how does GridWatch handle it?
7. Why does ingest wait for Kafka's answer before returning 202?
8. What is a dead-letter topic for?
9. Why does a poison message block a partition without one?
10. Why `ErrorHandlingDeserializer`?

### RabbitMQ

1. What are exchange, queue and binding? Which one decides the routing?
2. Topic vs direct vs fanout exchange?
3. What do `*` and `#` match?
4. Why does the publisher not know the queue?
5. What is an ack, and when does Spring send it?
6. What happens by default when a listener throws?
7. How does a message reach the DLQ in GridWatch?
8. Why `AmqpRejectAndDontRequeueException`?
9. Why publish after commit? What gap remains?
10. Why does GridWatch use both Kafka and RabbitMQ?

### Testing

1. Unit, web and integration test: what is real in each?
2. `@Mock` vs `@MockitoBean`?
3. When do you need `when(...)`?
4. Why `doThrow` for a void method?
5. What should `verify` check, and what not?
6. What does `@ServiceConnection` do?
7. Why are `@Container` fields static and `@Autowired` fields not?
8. Why wait before asserting in an IT?
9. Why can't a mock test prove the rollback?
10. `mvn test` vs `mvn verify`?

### Flyway

1. Why Flyway instead of `ddl-auto: update`?
2. What does `ddl-auto: validate` do?
3. What is `flyway_schema_history`?
4. Why never edit a migration that already ran?
5. What was wrong with `baseline-on-migrate` here?
6. How do you rename a column with no downtime?
7. What happens if a migration fails halfway?
8. What happens when 2 instances start at once?
9. Why does each service have its own migrations?
10. How do you undo a bad migration in production?

### Prometheus and Grafana

1. Logs vs metrics?
2. Why does Prometheus pull?
3. Counter vs gauge vs timer?
4. What does `rate()` do, and why not just use the counter?
5. Why does a counter appear only after the first increment?
6. Why count after success?
7. What is high cardinality, and why avoid it?
8. Why provision Grafana from files?
9. Why is `/actuator/prometheus` open, and is that OK in production?
10. What would you alert on first, and why?

### Docker and CI

1. Image vs container?
2. Why a two-stage Dockerfile?
3. Why `kafka:29092` in Docker but `localhost:9092` in IntelliJ?
4. Bind mount vs named volume?
5. What does `down -v` do differently?
6. What does `depends_on` with `service_healthy` add?
7. Why is `.env` not in Git, and `.env.example` is?
8. Why a matrix in CI?
9. Why did `mvnw` need the executable bit?
10. Why build images in CI without publishing them?

### Your decisions

1. Walk through one reading, from POST to email.
2. Where can a message be lost, and where can it be duplicated?
3. How do you get exactly one email?
4. What if the app crashes after sending the email but before the commit?
5. What if the mail server is down for an hour?
6. What if notifier is down for an hour?
7. What is the first thing that breaks at 100 times the load?
8. Why does each service own its database?
9. What would you change if you started again?
10. Which part did you find hardest, and how did you solve it?

## Part 3: Answers

Short answers. Say them in your own words.

### Kafka

1. A topic is a named stream. It is split into partitions, each an ordered log; partitions give parallelism.
2. Same key goes to the same partition, so readings of one substation stay in order.
3. A message's position in a partition. Kafka stores the committed offset per consumer group.
4. 2 consumers: one gets 2 partitions, the other 1. 5 consumers: 3 work, 2 are idle.
5. A rebalance gives its partitions to the others, which continue from the last committed offset.
6. A message can arrive twice but isn't lost. alert skips reading UUIDs it already saved.
7. So 202 means Kafka really has it. If Kafka is down, the client gets 503 and can retry.
8. A place for messages that keep failing, so the partition can move on and a human can inspect them.
9. Order within a partition means the consumer can't pass it, so it retries it forever.
10. A bad message fails in deserialization, before your code. It catches that and passes it to the error handler instead of looping.

### RabbitMQ

1. Exchange receives, queue stores, binding is the rule between them. The exchange routes, using the bindings.
2. Topic: patterns. Direct: exact key. Fanout: every bound queue, ignoring the key.
3. `*` one word, `#` zero or more words.
4. So new consumers can be added without changing the publisher.
5. "Done, delete it." Spring sends it when the listener returns normally.
6. The message is requeued and redelivered, forever if it always fails.
7. Retries run out, the message is rejected without requeue, and the queue's dead-letter settings send it to the DLQ.
8. It marks a permanent failure: no retry, straight to the DLQ. The customizer makes retries respect it.
9. So no message goes out for a rolled-back work order. A crash between commit and send loses it; a transactional outbox fixes that.
10. Readings are facts that can be replayed and read by many; a notification is a job done once with retries.

### Testing

1. Unit: one class. Web: controller, JSON, validation, security. Integration: the whole service with real infrastructure.
2. `@Mock` is plain Mockito. `@MockitoBean` puts the fake inside Spring.
3. When the default answer (`null`, `false`) is wrong for the test.
4. There is no return value to put inside `when`, so it doesn't compile.
5. Effects: save, send, publish. Not lookups, which are how the method decides.
6. It points Spring at the container, so no URLs or passwords are needed.
7. One container per class, started before Spring. Spring injects into the test object and skips statics.
8. The listener runs on another thread, so the result isn't there yet.
9. A mock has no database or transaction. Only a real database can show nothing was saved.
10. `test` runs `*Test`. `verify` also runs `*IT`.

### Flyway

1. Changes are versioned, reviewed and repeatable. Hibernate guesses, and never handles data changes well.
2. It checks that entities match the tables at startup and fails if they don't. It changes nothing.
3. The table recording which migrations ran, with checksums.
4. The checksum no longer matches and startup fails, and other databases would differ. Add a new migration.
5. It marked V1 as already done on an existing database, so the script was never really tested.
6. Add the new column, write to both, copy the data, move reads, then drop the old one, across several releases.
7. On PostgreSQL the transaction rolls back and the app won't start. Flyway records the failure; fix it, then `repair` if needed.
8. Flyway takes a database lock, so one runs and the other waits.
9. Each service owns its own data, so schemas change independently.
10. Write a new forward migration that fixes it. Keep a backup for real disasters.

### Prometheus and Grafana

1. Logs tell what happened to one event; metrics show numbers over time.
2. It controls timing, sees at once when a service is gone, and services don't need to know it.
3. Counter only goes up; gauge goes up and down; timer measures duration.
4. The speed of a counter per second. The total only ever rises, and `rate` also handles restarts.
5. Micrometer creates it on first use. Until then nothing exists to publish.
6. So it counts what really happened.
7. Too many tag values, such as one per reading id. Each is a separate series, which eats memory.
8. It lives in Git, repeats anywhere, and survives a recreated container.
9. Prometheus doesn't log in. In production, keep it on an internal network or management port.
10. Symptoms users feel: 5xx rate, then queue depth or lag. Alert on impact, not on every cause.

### Docker and CI

1. An image is the template; a container is a running copy.
2. A JDK builds the jar, and only a small JRE ships, so the image is smaller and safer.
3. Inside Docker, the service name is the address; from the laptop, it is the published port on localhost. Kafka has one listener for each.
4. Bind mount: a path on your laptop, for config. Named volume: Docker-managed, for data.
5. It also deletes named volumes, so all data is gone.
6. It waits until the dependency passes its healthcheck, not just until it started.
7. `.env` holds real values. `.env.example` shows which names are needed.
8. One job per service, without repeating the steps; they run in parallel.
9. Windows doesn't store that flag, and Linux refuses to run the script without it.
10. It proves the Dockerfiles work. Nothing needs to pull them until week 6.

### Your decisions

1. POST to ingest → validate → Kafka with key → 202 → alert consumes → duplicate check → save → above the limit, work order → commit → RabbitMQ → exchange → queue → notifier → save id + email in one transaction → ack.
2. Lost: crash between commit and publish. Duplicated: Kafka redelivery, RabbitMQ redelivery; both handled by idempotent consumers.
3. At-least-once delivery plus an idempotent consumer: the work order id in `sent_notification`.
4. The save rolls back, the retry sends a second email. Accepted: email can't join a database transaction.
5. 3 attempts, then the DLQ. Messages wait there for a human to move them back, so longer retries could help.
6. Messages wait in the queue, and are handled on restart.
7. Your own answer, with a reason. Good candidates: single alert instance and one PostgreSQL, one write per reading.
8. Independent changes and clear ownership. The cost: no joins across services.
9. Your own answer. Be specific.
10. Your own answer. Name the problem, what you tried, and what you learned.
