package com.hapi.gridwatch.alert;


import com.hapi.gridwatch.alert.dto.ReadingRestRequest;
import com.hapi.gridwatch.alert.repository.ReadingRepository;
import com.hapi.gridwatch.alert.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
public class AlertFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ReadingRepository readingRepository;

    @Autowired
    WorkOrderRepository workOrderRepository;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;


    @BeforeEach
    void binding() {
        amqpAdmin.declareQueue(new Queue("test.work-orders"));
        amqpAdmin.declareBinding(new Binding("test.work-orders", Binding.DestinationType.QUEUE, "gridwatch.work-orders", "#", null));
        workOrderRepository.deleteAll();
        readingRepository.deleteAll();
        amqpAdmin.purgeQueue("test.work-orders", false);
    }

    private ReadingRestRequest send(String sensorType, double value) {
        ReadingRestRequest reading = new ReadingRestRequest(UUID.randomUUID(), "DEV-01", sensorType, value, Instant.now());
        kafkaTemplate.send("gridwatch.readings", "DEV-01", reading);
        return reading;
    }
    private ReadingRestRequest send(ReadingRestRequest request) {
        kafkaTemplate.send("gridwatch.readings", "DEV-01", request);
        return request;
    }
    @Test
    void readingAboveLimitCreatesWorkOrderAndPublishes() {
        ReadingRestRequest reading = send("TEMPERATURE", 95.0);
        await().atMost(Duration.ofSeconds(15)).until(() -> readingRepository.existsById(reading.id()));
        assertThat(workOrderRepository.count()).isEqualTo(1);


        // Fake notifier service
        Message message = rabbitTemplate.receive("test.work-orders", 2000);
        assertThat(message).isNotNull();
        assertThat(new String(message.getBody())).contains("\"substationId\":\"DEV-01\"");
    }

    @Test
    void readingBelowLimitCreatesNoWorkOrder(){
        ReadingRestRequest reading = send("TEMPERATURE", 50D);
        await().atMost(Duration.ofSeconds(15)).until(() -> readingRepository.existsById(reading.id()));
        assertThat(workOrderRepository.count()).isEqualTo(0);

        Message message = rabbitTemplate.receive("test.work-orders", 2000);
        assertThat(message).isNull();
    }
    @Test
    void duplicateReadingIsSavedOnce(){
        ReadingRestRequest request = new ReadingRestRequest(UUID.randomUUID(), "DEV-01", "TEMPERATURE", 95.0, Instant.now());
        send(request);
        send(request);
        await().pollDelay(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(15))
                .until(() -> readingRepository.existsById(request.id()));

        assertThat(readingRepository.count()).isEqualTo(1);
        assertThat(workOrderRepository.count()).isEqualTo(1);
        Message message = rabbitTemplate.receive("test.work-orders", 2000);
        assertThat(message).isNotNull();
        assertThat(new String(message.getBody())).contains("\"substationId\":\"DEV-01\"");
    }

}
