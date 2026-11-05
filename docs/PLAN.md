# GridWatch plan

Week by week, from the learning plan (version 4). `[x]` = done and verified, `[ ]` = open. Dates are the plan's targets.

**Status 30 Oct 2026:** weeks 0-5 are built and tested. The repo stays private for now. Next: self-test, then week 6.

Tutorials (PDFs outside this repo, in the personal `Learning` folder): Kafka Tutorial, RabbitMQ Tutorial, Project Learning Plan.

## Week 0: Setup

Notes: [week-00-setup.md](weeks/week-00-setup.md) · Reference: [git-cheatsheet.md](git-cheatsheet.md), [docker-basics.md](docker-basics.md)

- [x] Java 21, Maven wrapper, Docker Desktop, IntelliJ
- [x] Repository structure and conventions

## Week 1: REST services (19-25 Oct)

Notes: [week-01-rest-services.md](weeks/week-01-rest-services.md) · Reference: [ARCHITECTURE.md](ARCHITECTURE.md)

- [x] ingest-service and alert-service (Java 21, Maven)
- [x] REST endpoint for readings
- [x] PostgreSQL and work orders
- [x] Spring Security (Basic Auth)
- [x] Done when: a reading from Postman gives a work order in the database

## Week 2: Kafka (26 Oct - 1 Nov)

Notes: [week-02-kafka.md](weeks/week-02-kafka.md) · Reference: [kafka-basics.md](kafka-basics.md), [kafka-cheatsheet.md](kafka-cheatsheet.md), [kafka-in-docker.md](kafka-in-docker.md), Kafka Tutorial

- [x] Kafka between ingest and alert, key = substationId
- [x] Anomaly rule: value above the limit opens a work order
- [x] Retries and dead-letter topic
- [x] Dockerfiles; one `docker compose up` starts everything
- [x] Done when: bad readings create work orders automatically

## Week 3: RabbitMQ (2-8 Nov)

Notes: [week-03-rabbitmq.md](weeks/week-03-rabbitmq.md) · Reference: [rabbitmq-basics.md](rabbitmq-basics.md), [rabbitmq-cheatsheet.md](rabbitmq-cheatsheet.md), [messaging-flow.md](messaging-flow.md), RabbitMQ Tutorial

- [x] notifier-service, exchange, queue, binding
- [x] Publish after commit
- [x] Retries, dead-letter queue, no retry for invalid data
- [x] One email per work order
- [x] Email through Mailpit
- [x] Done when: stop the notifier, create alerts, start it again, and no notification is lost

## Week 4: Flyway and tests (9-15 Nov)

Notes: [week-04-testing.md](weeks/week-04-testing.md) · Reference: [testing-cheatsheet.md](testing-cheatsheet.md)

- [x] Flyway migrations for alert and notifier
- [x] Unit tests: ingest 3, alert 5, notifier 3
- [x] Web tests: ingest 5
- [x] Integration tests with real containers: ingest 1, alert 3, notifier 2
- [x] Done when: all tests pass for all three services

## After week 4: make the repo public

Reference: [docker-basics.md](docker-basics.md)

- [x] Passwords moved to `.env` and environment variables
- [x] Confirm the flow with `.env` (one reading, one email) and commit
- [x] Add the `.env` setup step to the README and week 0 notes
- [x] Check the repo for anything that should not be public
- [ ] Make the repo public and pin it (private for now, by choice)
- [ ] Add "Currently building GridWatch" to the GitHub profile README

## Week 5: CI and monitoring (16-22 Nov)

Notes: [week-05-ci-monitoring.md](weeks/week-05-ci-monitoring.md)

Reference: [monitoring-cheatsheet.md](monitoring-cheatsheet.md), [testing-cheatsheet.md](testing-cheatsheet.md), [docker-basics.md](docker-basics.md)

- [x] GitHub Actions: `verify` for all three services on every push
- [x] Mark `mvnw` as executable in Git
- [x] GitHub Actions: build the Docker images
- [x] Actuator + Micrometer metrics
- [x] Prometheus in Docker Compose
- [x] Grafana dashboard: readings per second, alerts, errors
- [x] Done when: green check on every push, and the dashboard shows readings per second and alert counts

## Self-test before week 6

Reference: [self-test.md](self-test.md)

- [x] 1. Flyway: add a column
- [x] 2. RabbitMQ: a second event
- [x] 3. RabbitMQ: a second consumer
- [ ] 4. Kafka: groups and instances
- [ ] 5. Kafka: lag
- [ ] 6. Testing: the failure paths
- [ ] 7. Grafana: see failures
- [ ] 8. The whole system
- [ ] All questions answered without looking

## Week 6: Kubernetes and AWS (23-29 Nov)

- [ ] Run on kind
- [ ] Helm chart
- [ ] 2 alert-service replicas
- [ ] One AWS free-tier deployment, billing alarm first
- [ ] Done when: deleting one pod does not stop alerts, and the public URL works

## Week 7: AI note (30 Nov - 6 Dec)

- [ ] Technician note with Spring AI
- [ ] Resilience4j circuit breaker around the AI call
- [ ] README with diagram and demo GIF
- [ ] Shut down AWS
- [ ] Done when: with the AI off, alerts still arrive without a note, and someone else can run it in 10 minutes

## Week 8: RAG and evaluation (7-13 Dec)

- [ ] Public maintenance documents in `manuals/`
- [ ] Chunks and embeddings in PostgreSQL with pgvector
- [ ] The note names the manual section it used; no match means no note
- [ ] `evals/`: 20 anomaly cases and a Python script
- [ ] README: hit rate, citation rate, measured cost
- [ ] README: "How I used AI tools"
- [ ] Done when: one command runs the eval and the report shows both numbers

## Extra: Azure (28 Dec - 3 Jan)

- [ ] Budget alert first
- [ ] Run the images on Azure Container Apps with a database
- [ ] One alert end to end, then delete the resource group
- [ ] README: "AWS vs Azure" note
- [ ] Done when: the public URL shows an alert, the resource group is deleted, and the note is in the README

## Extra, not assigned to a week

- [x] Sensor simulator: `docker compose --profile simulator up -d --build`
