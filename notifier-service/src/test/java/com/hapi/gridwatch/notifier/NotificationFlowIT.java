package com.hapi.gridwatch.notifier;

import com.hapi.gridwatch.notifier.dto.WorkOrderCreatedEvent;
import com.hapi.gridwatch.notifier.repository.SentNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@Testcontainers
public class NotificationFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @MockitoBean
    JavaMailSender mailSender;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    SentNotificationRepository sentNotificationRepository;

    private void send(Long workOrderId) {
        WorkOrderCreatedEvent event = new WorkOrderCreatedEvent(workOrderId, "DEV-01", "LOAD 130 above limit 100", Instant.now());
        rabbitTemplate.convertAndSend("gridwatch.work-orders", "work-order.created", event);
    }

    @Test
    void messageIsSavedAndEmailed() {
        send(100L);

        await().atMost(Duration.ofSeconds(10)).until(() -> sentNotificationRepository.existsById(100L));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void failedEmailRollsBackSave() {
        doThrow(new MailSendException("down")).when(mailSender).send(any(SimpleMailMessage.class));

        send(200L);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> verify(mailSender, times(3)).send(any(SimpleMailMessage.class)));
        assertThat(sentNotificationRepository.existsById(200L)).isFalse();
    }
}
