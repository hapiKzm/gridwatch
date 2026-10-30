# Monitoring cheat sheet (GridWatch)

How the numbers travel from the code to a graph, and the queries and commands we use. Week log: [week-05-ci-monitoring.md](weeks/week-05-ci-monitoring.md).

## 1. The big idea

```
your code → /actuator/prometheus → Prometheus → Grafana
 counts       shows the number      keeps history   draws it
             now
```

| Piece | Job |
|---|---|
| Micrometer | Library the code uses to count |
| Actuator | Publishes the numbers at a URL |
| Prometheus | Visits that URL every 5 s and stores the history |
| Grafana | Draws graphs from Prometheus |

## 2. Logs vs metrics

| | Logs | Metrics |
|---|---|---|
| Looks like | Sentences about single events | Numbers over time |
| Answers | What happened to reading X? | How many per second? Getting worse? |

## 3. Addresses

| What | Address |
|---|---|
| Metrics pages | http://localhost:8081/actuator/prometheus, also 8082, 8083 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000, user `admin`, password `GRAFANA_PASSWORD` from `.env` |

Inside Docker, use service names: `alert:8081`, `prometheus:9090`.

## 4. Counting in code

```java
meterRegistry.counter("gridwatch.readings.received").increment();
meterRegistry.counter("gridwatch.work_orders.created", "sensor", "LOAD").increment();
```

- Count after the success.
- A counter appears on the page only after its first `increment()`.
- Dots become `_`, and counters get `_total`.
- `created` is dropped by the Prometheus library: `gridwatch_work_orders_total`.
- Keep tag values few (sensor type: yes; reading id: no). Every new value is a new series.

| Type | Use |
|---|---|
| Counter | Only goes up: readings, errors |
| Gauge | Goes up and down: memory, queue size |
| Timer | How long something takes: `http_server_requests_seconds` |

In unit tests:

```java
@Spy MeterRegistry meterRegistry = new SimpleMeterRegistry();   // with @InjectMocks
new ReadingService(kafka, "topic", new SimpleMeterRegistry());  // by hand
```

## 5. PromQL queries

| Question | Query |
|---|---|
| Readings per second | `rate(gridwatch_readings_received_total[1m])` |
| Work orders per sensor | `sum by (sensor) (gridwatch_work_orders_total)` |
| Work orders in the last hour | `increase(gridwatch_work_orders_total[1h])` |
| 5xx errors per second | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m]))` |
| Is a service up? | `up` |

`rate` = how fast a counter rose per second over the window. It also handles a counter going back to 0 after a restart.

## 6. Grafana as files

| File | Purpose |
|---|---|
| `monitoring/grafana/provisioning/datasources/prometheus.yml` | Where Prometheus is |
| `monitoring/grafana/provisioning/dashboards/dashboards.yml` | Where to find dashboards |
| `monitoring/grafana/dashboards/gridwatch.json` | The dashboard itself |

To change the dashboard: edit in Grafana, export JSON, replace `gridwatch.json`, commit.

## 7. Test the error panel

```bash
docker compose stop kafka
```

Send a reading: expect 503. Then:

```bash
docker compose start kafka
```

## 8. When something is wrong

| Symptom | Look at |
|---|---|
| Target down | Prometheus → Status → Target health; is the service up? |
| `401` on the metrics page | `/actuator/prometheus` in `permitAll` |
| Metric not on the page | Nothing counted yet: send a reading |
| Panel "No data" | Refresh, time range, exact metric name; errors panel is empty until a 5xx |
| Dashboard gone | JSON not in `monitoring/grafana/dashboards/` |
