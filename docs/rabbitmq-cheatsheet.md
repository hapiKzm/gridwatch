# RabbitMQ cheat sheet (GridWatch)

Settings and code GridWatch uses, commands, and the traps we hit. Concepts: [rabbitmq-basics.md](rabbitmq-basics.md). Whole flow: [messaging-flow.md](messaging-flow.md).

## Names

| Thing | Name | Declared by |
|---|---|---|
| Exchange (topic) | `gridwatch.work-orders` | alert and notifier |
| Routing key | `work-order.created` | alert sends it |
| Queue | `notifier.work-orders` | notifier |
| Binding | `work-order.*` | notifier |
| Dead-letter queue | `notifier.work-orders.dlq` | notifier |
| Message | `{"workOrderId":42,"substationId":"DEV-01","reason":"...","createdAt":"..."}` | — |

## 1. Docker (`docker-compose.yml`)

```yaml
rabbitmq:
  image: rabbitmq:4-management-alpine
  ports:
    - "5672:5672"      # apps
    - "15672:15672"    # dashboard
  environment:
    RABBITMQ_DEFAULT_USER: gridwatch
    RABBITMQ_DEFAULT_PASS: gridwatch-local
  volumes:
    - rabbitmq:/var/lib/rabbitmq
  healthcheck:
    test: ["CMD-SHELL", "rabbitmq-diagnostics -q ping"]
    interval: 10s
    timeout: 10s
    retries: 10
mailpit:
  image: axllent/mailpit:v1.27
  ports:
    - "1025:1025"      # SMTP
    - "8025:8025"      # inbox
```

In Docker the services use `SPRING_RABBITMQ_HOST: rabbitmq` and `SPRING_MAIL_HOST: mailpit`.

Dependency in both services: `spring-boot-starter-amqp`. Notifier also: `spring-boot-starter-mail`, `spring-boot-starter-data-jpa`, `postgresql`.

## 2. Publisher: alert-service

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: gridwatch
    password: gridwatch-local
    publisher-confirm-type: correlated   # broker confirms each message
    publisher-returns: true              # report messages no queue took
    template:
      mandatory: true
rabbit:
  exchange: gridwatch.work-orders
  routing-key: work-order.created
```

```java
@Configuration
public class RabbitConfig {
    @Bean
    public TopicExchange workOrderExchange(@Value("${rabbit.exchange}") String exchange) {
        return new TopicExchange(exchange);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
```

Send only after the database commit:

```java
// ReadingService, inside the @Transactional method
applicationEventPublisher.publishEvent(new WorkOrderCreatedEvent(...));

// WorkOrderPublisher
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void workOrderCreated(WorkOrderCreatedEvent event) {
    rabbit.convertAndSend(exchange, routingKey, event);
    log.info("Published work order {} to RabbitMQ", event.workOrderId());
}
```

`@TransactionalEventListener` only works when Spring delivers the event (`publishEvent`). Calling the method directly sends inside the transaction.

## 3. Listener: notifier-service

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: gridwatch
    password: gridwatch-local
    listener:
      simple:
        default-requeue-rejected: false   # after the last failure: no requeue → DLQ
        retry:
          enabled: true
          max-attempts: 3                 # 1 try + 2 retries
          initial-interval: 1s
          multiplier: 2                   # 1 s, 2 s
  mail:
    host: localhost
    port: 1025
rabbit:
  exchange: gridwatch.work-orders
  queue: notifier.work-orders
  binding-key: work-order.*
  dlq: notifier.work-orders.dlq
```

```java
@Bean
public Queue notifierQueue(@Value("${rabbit.queue}") String queue, @Value("${rabbit.dlq}") String dlq) {
    return QueueBuilder.durable(queue)
            .deadLetterExchange("")          // default exchange
            .deadLetterRoutingKey(dlq)       // = DLQ name
            .build();
}

@Bean
public Queue notifierDlq(@Value("${rabbit.dlq}") String dlq) {
    return QueueBuilder.durable(dlq).build();
}

@Bean
public Binding notifierBinding(Queue notifierQueue, TopicExchange workOrderExchange,
                               @Value("${rabbit.binding-key}") String bindingKey) {
    return BindingBuilder.bind(notifierQueue).to(workOrderExchange).with(bindingKey);
}

@Bean
public RabbitListenerRetrySettingsCustomizer skipRetryForInvalidMessages() {
    return settings -> settings.setExceptionPredicate(e ->
            !(e instanceof AmqpRejectAndDontRequeueException
                    || e.getCause() instanceof AmqpRejectAndDontRequeueException));
}
```

Plus the same exchange bean and JSON converter as alert.

```java
@RabbitListener(queues = "${rabbit.queue}")
public void receive(WorkOrderCreatedEvent event) {
    validate(event);                       // missing field → AmqpRejectAndDontRequeueException → DLQ, no retry
    notificationService.notifyOnce(event);
}

@Transactional
public void notifyOnce(WorkOrderCreatedEvent event) {
    if (sentNotificationRepository.existsById(event.workOrderId())) {
        log.info("Already notified for work order {}", event.workOrderId());
        return;
    }
    sentNotificationRepository.save(new SentNotificationEntity(event.workOrderId()));
    emailService.sendWorkOrderCreated(event);   // throws → save rolled back → retry
}
```

## 4. What happens on failure

| Case | Result |
|---|---|
| Mail server down | 3 attempts (1 s, 2 s) → DLQ, no row in `sent_notification` |
| Missing field | Straight to DLQ, no retry |
| Same work order twice | Second one skipped, one email |
| Notifier down | Messages wait in `notifier.work-orders` |
| No queue bound | Message dropped (returned + logged because of `mandatory`) |

## 5. Commands

```bash
docker compose exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
docker compose exec rabbitmq rabbitmqctl list_exchanges name type
docker compose exec rabbitmq rabbitmqctl list_bindings
docker compose exec rabbitmq rabbitmqctl purge_queue notifier.work-orders.dlq
docker compose exec rabbitmq rabbitmqctl delete_queue notifier.work-orders
docker compose exec db psql -U gridwatch -d notifier -c "select * from sent_notification;"
```

Dashboard http://localhost:15672 (`gridwatch` / `gridwatch-local`): Queues → Get messages to peek, Purge to empty, Exchanges → Publish message for manual tests (set `content_type` = `application/json`). Mailpit inbox: http://localhost:8025.

## 6. Traps we hit

| Symptom | Cause | Fix |
|---|---|---|
| Message published, queue empty | No binding matched the routing key | Check bindings; `#` matches everything for debugging |
| Same message every few ms, CPU high | Failure + default requeue | `default-requeue-rejected: false`, retry limit, DLQ |
| Failed messages vanish | DLQ routing key wrong (placeholder text, name mismatch) | One `rabbit.dlq` property for both queue argument and DLQ name |
| `PRECONDITION_FAILED inequivalent arg 'x-dead-letter-exchange'` | Queue existed without DLQ settings | Delete the queue, restart the app |
| Invalid message still retried 3 times | `AmqpRejectAndDontRequeueException` is wrapped and retried by default | `RabbitListenerRetrySettingsCustomizer` predicate that checks the cause |
| Topic name / exchange `null` | `@Value` on a `static` field | Constructor or `@Bean` parameter |
| Sent before commit | Publisher method called directly | `publishEvent` + `@TransactionalEventListener(AFTER_COMMIT)` |
| Duplicate email still sent | Missing `return` after the "already notified" check; `save` with an existing id updates, no error | Return early |
| Notifier can't connect | Wrong password, or `localhost` inside Docker | Check credentials; `SPRING_RABBITMQ_HOST: rabbitmq` |
| Queue consumed by the wrong service | Queue/binding/listener declared in the publisher | Consumer owns queue, binding, DLQ |
