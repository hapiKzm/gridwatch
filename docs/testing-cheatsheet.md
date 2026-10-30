# Testing cheat sheet (GridWatch)

What we test, why, and how each kind of test works, using the real tests in this repo. Week log: [week-04-testing.md](weeks/week-04-testing.md).

## 1. The big idea

A test answers one question: **"if someone breaks this later, will I find out?"**

No single kind of test can answer that for everything, so we use three. Each one makes a different part real and fakes the rest.

| Kind | What is real | What is fake | Speed | Docker | Answers |
|---|---|---|---|---|---|
| Unit test | One class | Everything it talks to | ms | No | Is the rule correct? |
| Web test | Controller, JSON, validation, security | The service behind it | seconds | No | Does the API answer with the right status? |
| Integration test | The whole service, database, brokers | Only the neighbouring services | ~1 min | Yes | Does it really work together? |

Think of a car:

- **Unit test:** check one part on a table, for example the brake pad.
- **Web test:** check the dashboard, with the engine faked.
- **Integration test:** start the car and drive it.

Why not only drive the car? It is slow, and when it fails you don't know which part broke. Why not only test parts? Every part can be fine while the car still doesn't drive, because a wire is missing.

## 2. What we have

| Service | Class | Kind | Tests | Protects |
|---|---|---|---|---|
| ingest | `ReadingServiceTest` | Unit | 3 | Sensor type check, Kafka key, Kafka failure |
| ingest | `ReadingControllerTest` | Web | 5 | 202, 400, 401, 503 |
| ingest | `IngestFlowIT` | Integration | 1 | POST ends up on real Kafka |
| alert | `ReadingServiceTest` | Unit | 5 | Limit rule, duplicates, invalid readings |
| alert | `AlertFlowIT` | Integration | 3 | Kafka → database → RabbitMQ |
| notifier | `NotificationServiceTest` | Unit | 3 | Send once, failure is passed on |
| notifier | `NotificationFlowIT` | Integration | 2 | RabbitMQ → database + email, rollback |

## 3. Every test has the same 3 parts

```java
// 1. SETUP: what must be true before?
when(readingRepository.existsById(any())).thenReturn(true);

// 2. ACT: do one thing
readingService.saveReadingRequest(reading("TEMPERATURE", 90.0));

// 3. CHECK: what should have happened?
verify(readingRepository, never()).save(any());
```

**How to know what to write:** read the method and follow the path for this case. What does the path need? That is the setup. What does it do to the outside world? Those are the checks.

## 4. Unit tests (Mockito)

**Why fakes?** `ReadingService` talks to a database. To test only its rule, we replace the database with a fake, so a failure can only mean the rule is wrong.

```java
@ExtendWith(MockitoExtension.class)        // turn Mockito on, no Spring
class ReadingServiceTest {
    @Mock ReadingRepository readingRepository;   // fake
    @InjectMocks ReadingService readingService;  // real class, gets the fakes
}
```

A fake answers every call with a default: `false`, `null`, or nothing.

### When do I need `when`?

Only when the default is wrong for this test.

| Call | Default | OK? | So |
|---|---|---|---|
| `existsById`, normal reading | `false` = not a duplicate | Yes | Nothing |
| `existsById`, duplicate test | `false` | No, we need a duplicate | `thenReturn(true)` |
| `workOrderRepository.save` | `null` | No, the code calls `.getId()` on it | `thenAnswer(call -> call.getArgument(0))` |
| `readingRepository.save` | `null` | Yes, the result is ignored | Nothing |

**Rule:** passing something **into** a fake is always safe. Using what **comes back** from a fake needs setup.

### Which form?

| Method | You want | Write |
|---|---|---|
| Returns a value | Give a value | `when(mock.call()).thenReturn(value)` |
| Returns a value | Throw | `when(mock.call()).thenThrow(error)` |
| `void` | Throw | `doThrow(error).when(mock).call()` |

`when(mock.voidMethod())` doesn't compile: there is no value to put inside `when`.

### What do I verify?

Check effects on the outside world, not how the method decided.

| Verify | Why |
|---|---|
| `save`, `send`, `publishEvent` | That is the job of the method |
| Not `existsById` | Only how it decides. If the code changes to `findById`, nothing is wrong, so the test should not break |

`verify(mock).call()` = it happened once. `verify(mock, never()).call(any())` = it never happened.

### Limit of unit tests

`emailFailureThrows` proves the error is passed on. It can't prove the database save was undone, because there is no database. That needs an integration test.

## 5. Web tests (`@WebMvcTest`)

**Why?** Status codes, validation and login are done by Spring around the controller. A unit test that calls the controller method directly skips all of that. `@WebMvcTest` runs that part for real, without starting Kafka.

```java
@WebMvcTest(ReadingController.class)   // only the web layer
@Import(WebSecurityConfig.class)       // our login rules, skipped otherwise
class ReadingControllerTest {
    @Autowired MockMvc mockMvc;        // sends fake HTTP requests, no real port
    @MockitoBean ReadingService readingService;   // fake inside Spring
}
```

```java
mockMvc.perform(post("/api/readings")                 // build the request
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON))                 // perform closes here
        .andExpect(status().isAccepted());            // check the answer
```

| Test | What triggers it | Expect |
|---|---|---|
| `noLoginReturns401` | No `@WithMockUser` | 401 |
| `validReadingReturns202` | Service returns an id | 202 + id |
| `missingFieldReturns400` | `@Valid` stops it; the service is never called | 400 |
| `unknownSensorReturns400` | Fake service throws | 400 |
| `kafkaDownReturns503` | Fake service throws | 503 |

**Common mix-up:** in `unknownSensorReturns400`, sending `WIND` does nothing, because the service is fake and the real check never runs. The test asks only: "if the service throws, does the controller answer 400?" The sensor rule itself is tested in `ReadingServiceTest`.

## 6. Integration tests (Testcontainers)

**Why?** Mocks can't find wrong JSON, wrong queue names, missing transactions or a broken Flyway script. These tests can.

```java
@SpringBootTest        // start the whole service, with real listeners
@Testcontainers        // JUnit starts and stops the containers
class AlertFlowIT {
    @Container
    @ServiceConnection // Spring connects here, no URL or password needed
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @Autowired KafkaTemplate<Object, Object> kafkaTemplate;
}
```

### The test plays the neighbours

Only one service is tested. The test pretends to be the services around it.

| Test class | Real service | Test plays | How |
|---|---|---|---|
| `IngestFlowIT` | ingest | Client and alert | `MockMvc` POST; own `KafkaConsumer` reads the topic |
| `AlertFlowIT` | alert | ingest and notifier | `kafkaTemplate.send`; own queue `test.work-orders` |
| `NotificationFlowIT` | notifier | alert and mail server | `rabbitTemplate.convertAndSend`; `@MockitoBean JavaMailSender` |

```
Real system:    Postman → ingest --Kafka--> alert → DB → RabbitMQ → notifier
AlertFlowIT:               TEST  --Kafka--> alert → DB → RabbitMQ → TEST queue
```

This is not mocking: Kafka, RabbitMQ and PostgreSQL are real. Don't copy the `@KafkaListener` into the test. The real one is already running, and a second consumer in the same group would steal messages.

### The test queue

alert has no queue, so its messages would be dropped. The test makes its own:

```java
amqpAdmin.declareQueue(new Queue("test.work-orders"));         // an empty mailbox
amqpAdmin.declareBinding(new Binding("test.work-orders", Binding.DestinationType.QUEUE,
        "gridwatch.work-orders", "#", null));                  // the rule: copy everything here
```

The queue has no key. The binding has the pattern, and `#` matches every key.

### Start each test clean

Containers are shared by all tests in a class, so old data can fail the next test.

```java
@BeforeEach
void setup() {
    amqpAdmin.purgeQueue("test.work-orders", false);  // empty the queue
    workOrderRepository.deleteAll();                  // children first (foreign key)
    readingRepository.deleteAll();
}
```

### Wait first, then check

`send` only drops the message on Kafka. Another thread handles it a moment later.

```java
ReadingRestRequest reading = send("TEMPERATURE", 95.0);
await().atMost(Duration.ofSeconds(15))
       .until(() -> readingRepository.existsById(reading.id()));   // wait
assertThat(workOrderRepository.count()).isEqualTo(1);              // then check
```

To prove something did **not** happen, a plain wait stops too early. In the duplicate test it ends after the first copy, so give the second copy time:

```java
await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(15)).until(...);
```

### Checking the message

```java
Message message = rabbitTemplate.receive("test.work-orders", 2000);
assertThat(new String(message.getBody())).contains("\"substationId\":\"DEV-01\"");
```

Use `receive`, not `receiveAndConvert`: the converter refuses classes outside its trusted packages. Reading the JSON also checks the contract notifier depends on.

## 7. Static or not?

| Thing | Static? | Why |
|---|---|---|
| `@Container` | Yes | One container for the class, Spring connects once |
| `@Autowired`, `@MockitoBean`, `@Mock` | Never | Injected into the test object; static stays `null` |
| `@BeforeEach` | No | Runs after injection |
| `@BeforeAll` | Must be static | Runs before injection, so it can't use `@Autowired` fields |

Test and setup methods can't be `private`.

## 8. How to run

| Command | Runs | Docker |
|---|---|---|
| `<service>/mvnw -f <service>/pom.xml clean test` | `*Test`, `*Tests` (surefire) | No |
| `<service>/mvnw -f <service>/pom.xml clean verify` | The above + `*IT` (failsafe) | Yes |
| `... clean test -Dtest=ReadingServiceTest` | One class | No |
| `... clean test "-Dtest=ReadingServiceTest#duplicateReadingIsSkipped"` | One method | No |

In PowerShell, put quotes around `-D` arguments that contain a dot, such as `"-Dit.test=AlertFlowIT"`.

## 9. Errors you will see

| Error | Meaning | Fix |
|---|---|---|
| `NoClassDefFoundError` at discovery | Old `target/` folder | `clean test` |
| `NullPointerException` in a unit test | Code used a fake's `null` | Add `when` / `thenAnswer` |
| `UnnecessaryStubbingException` | `when` the test never used | Delete it |
| `@Autowired` field is `null` | Field is `static` | Remove `static` |
| `No qualifying bean of type` | Wrong generic type, or not in a `@WebMvcTest` | Use the real type, or remove the field |
| `Could not find a valid Docker environment` | Docker is off | Start Docker Desktop |
| `Can't get Docker image` | Usually Docker is off | Start Docker Desktop |
| `Tests run: 0` / IT skipped | No failsafe plugin, or `mvn test` | Add failsafe, run `verify` |
| `Could not find goal 'integration-test'` | Failsafe goals inside the Spring Boot plugin | Separate `<plugin>` |
| `'beans' must not be empty` | Mail health check with fake mail sender | `management.health.mail.enabled=false` |
| `not in the trusted packages` | `receiveAndConvert` | Use `receive` |
| `The condition to evaluate should not be null` | `assertThat(x).is(null)` | `isNull()` |
| Got 415 instead of 400 | No content type | `.contentType(MediaType.APPLICATION_JSON)` |
| `MediaType` doesn't fit | JUnit's class imported | `org.springframework.http.MediaType` |
