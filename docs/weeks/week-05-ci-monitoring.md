# Week 5: CI and monitoring

## Goal

Every push is built and tested automatically, and the running system shows how it is doing: readings per second, work orders and errors, on one dashboard.

## What was built

### CI (`.github/workflows/ci.yml`)
- Runs on every push to `main` and on pull requests.
- One job per service, with a matrix; `fail-fast: false` so one failure doesn't cancel the others.
- Steps: checkout, Java 21 with Maven cache, `./mvnw -B verify`, `docker build`.
- Testcontainers works on `ubuntu-latest` because Docker is already installed.
- `mvnw` marked as executable in Git (`update-index --chmod=+x`).

### Metrics
- `micrometer-registry-prometheus` in all three services; `/actuator/prometheus` exposed without login.
- notifier got `spring-boot-starter-webmvc`: without a web server it has no URL to scrape. It still has no API.
- Own counters through `MeterRegistry`:

| Service | In code | In Prometheus | Added when |
|---|---|---|---|
| ingest | `gridwatch.readings.received` | `gridwatch_readings_received_total` | Kafka confirmed the reading |
| alert | `gridwatch.work_orders.created` + tag `sensor` | `gridwatch_work_orders_total{sensor="..."}` | Work order saved |
| notifier | `gridwatch.notifications.sent` | `gridwatch_notifications_sent_total` | Email sent |

### Prometheus and Grafana
- `monitoring/prometheus.yml`: scrapes `alert:8081`, `ingest:8082`, `notifier:8083` every 5 s.
- Grafana data source and dashboard loaded from files in `monitoring/grafana/`, so they survive a recreated container.
- Dashboard: readings per second, work orders by sensor, errors per second.

## Verified behaviour

| Test | Result |
|---|---|
| Push to `main` | 3 green jobs, tests and Docker image build |
| One reading above the limit | All three counters at 1 |
| Prometheus targets | 3 of 3 up |
| Kafka stopped, reading sent | 503, and 5xx count rose in Prometheus |
| Grafana container recreated | Data source and dashboard came back from files |

Details and queries: [monitoring-cheatsheet.md](../monitoring-cheatsheet.md).

## Design decisions

| Decision | Reason | Alternative |
|---|---|---|
| `verify` in CI | Integration tests catch wiring problems | `test`: faster, misses them |
| Build images in CI, don't publish yet | Catches broken Dockerfiles; nothing pulls images before week 6 | Publish to a registry now |
| Prometheus pulls | It decides the timing, sees at once when a service stops, and services don't need to know it | Services push |
| Counters, not gauges | Only go up; `rate()` gives per second and survives restarts | Gauge with a current value |
| Count after success | Counts what really happened | Count on arrival |
| Grafana config as files | In Git, repeatable | Clicking in the UI: lost with the container |
| `/actuator/prometheus` without login locally | Prometheus doesn't log in; shows no secrets | In production: internal network or separate management port |

## Problems solved along the way

| Problem | Cause | Fix |
|---|---|---|
| YAML ignored or wrong | Indentation, `-main` without a space | Keys under their parent; `- main` |
| Port 8083 answers nothing | notifier had no web server | Add `spring-boot-starter-webmvc` |
| `/actuator/prometheus` gives 401 | Not in `permitAll` | Add it next to `/actuator/health/**` |
| `@InjectMocks` has no `MeterRegistry` | New constructor argument | `@Spy MeterRegistry = new SimpleMeterRegistry()` |
| `gridwatch_work_orders_created_total` not found | Prometheus library drops the reserved `created` | Use `gridwatch_work_orders_total` |
| Prometheus config not loaded | `mount` is not a Compose key | `volumes` |
| Dashboard gone after recreating Grafana | Built by hand, in the container | Export JSON, load from file |
| Dashboard file not found | JSON in the provisioning folder | Move to `monitoring/grafana/dashboards/` |

## Commits

`ci: run tests for all services on github actions` → `ci: build docker images` → `feat: expose prometheus metrics` → `feat: count readings, work orders and notifications` → `feat: add prometheus to docker compose` → `feat: add grafana with prometheus data source` → `feat: load grafana dashboard from file`
