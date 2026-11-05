package com.hapi.gridwatch.notifier;

import com.hapi.gridwatch.notifier.dto.WorkOrderEvent;
import com.hapi.gridwatch.notifier.model.SentNotificationPkEntity;
import com.hapi.gridwatch.notifier.repository.SentNotificationRepository;
import com.hapi.gridwatch.notifier.service.AuditService;
import com.hapi.gridwatch.notifier.types.WorkOrderType;
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
import java.util.Locale;

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

    @MockitoBean
    AuditService auditService;

    private void send(Long workOrderId, WorkOrderType workOrderType) {
        WorkOrderEvent event = new WorkOrderEvent(workOrderId, "DEV-01", "LOAD 130 above limit 100", Instant.now(), workOrderType);
        rabbitTemplate.convertAndSend("gridwatch.work-orders", "work-order." + workOrderType.toString().toLowerCase(Locale.ROOT), event);
    }

    private SentNotificationPkEntity createSentNotificationPkEntity(Long workOrderId, WorkOrderType workOrderType) {
        return new SentNotificationPkEntity(
                workOrderId,
                workOrderType
        );
    }

    @Test
    void messageIsSavedAndEmailed() {
        send(100L, WorkOrderType.OPEN);

        SentNotificationPkEntity sentNotificationPkEntity = createSentNotificationPkEntity(100L, WorkOrderType.OPEN);
        await().atMost(Duration.ofSeconds(10)).until(() -> sentNotificationRepository.existsById(sentNotificationPkEntity));
        verify(mailSender).send(argThat((SimpleMailMessage m) -> m.getSubject().contains("100")));
    }
    @Test
    void failedEmailRollsBackSave() {
        doThrow(new MailSendException("down")).when(mailSender).send(any(SimpleMailMessage.class));

        send(200L, WorkOrderType.OPEN);
        SentNotificationPkEntity sentNotificationPkEntity = createSentNotificationPkEntity(200L, WorkOrderType.OPEN);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> verify(mailSender, times(3)).send(argThat((SimpleMailMessage m) -> m.getSubject().contains("200"))));
        assertThat(sentNotificationRepository.existsById(sentNotificationPkEntity)).isFalse();
    }


    @Test
    void createdAndClosedForOneWorkOrderSendTwoEmails() {
        send(300L, WorkOrderType.OPEN);
        send(300L, WorkOrderType.CLOSED);

        SentNotificationPkEntity createdKey = createSentNotificationPkEntity(300L, WorkOrderType.OPEN);
        SentNotificationPkEntity closedKey = createSentNotificationPkEntity(300L, WorkOrderType.CLOSED);

        await().atMost(Duration.ofSeconds(10)).until(() ->
                sentNotificationRepository.existsById(createdKey) && sentNotificationRepository.existsById(closedKey));
        verify(mailSender, times(2)).send(argThat((SimpleMailMessage m) -> m.getSubject().contains("300")));
    }

    @Test
    void failedClosedEmailRollsBackSave() {
        doThrow(new MailSendException("down")).when(mailSender).send(any(SimpleMailMessage.class));

        send(400L, WorkOrderType.CLOSED);
        SentNotificationPkEntity sentNotificationPkEntity = createSentNotificationPkEntity(400L, WorkOrderType.CLOSED);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> verify(mailSender, times(3)).send(argThat((SimpleMailMessage m) -> m.getSubject().contains("400"))));
        assertThat(sentNotificationRepository.existsById(sentNotificationPkEntity)).isFalse();
    }

    @Test
    void eventAlsoReachesAuditQueue() {
        send(500L, WorkOrderType.OPEN);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(auditService).saveLog(argThat(e -> e.workOrderId().equals(500L))));
    }
}
