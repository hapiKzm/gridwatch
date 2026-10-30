package com.hapi.gridwatch.notifier.service;


import com.hapi.gridwatch.notifier.dto.WorkOrderCreatedEvent;
import com.hapi.gridwatch.notifier.mail.EmailService;
import com.hapi.gridwatch.notifier.repository.SentNotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {
    @Mock
    SentNotificationRepository sentNotificationRepository;

    @Mock
    EmailService emailService;

    @InjectMocks
    NotificationService notificationService;

    @Spy
    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private WorkOrderCreatedEvent workOrderCreatedEvent(){
        return new WorkOrderCreatedEvent(
                1L,
                "DEV-01",
                "LOAD 130 above limit 100",
                Instant.now()
        );
    }

    @Test
    void newWorkOrderIsSavedAndEmailed(){
        WorkOrderCreatedEvent workOrderCreatedEvent = workOrderCreatedEvent();
        notificationService.notifyOnce(workOrderCreatedEvent);
        verify(sentNotificationRepository).save(any());
        verify(emailService).sendWorkOrderCreated(workOrderCreatedEvent);
    }

    @Test
    void alreadyNotifiedSendsNoEmail(){
        when(sentNotificationRepository.existsById(1L)).thenReturn(true);

        WorkOrderCreatedEvent workOrderCreatedEvent = workOrderCreatedEvent();
        notificationService.notifyOnce(workOrderCreatedEvent);
        verify(sentNotificationRepository, never()).save(any());
        verify(emailService, never()).sendWorkOrderCreated(any());
    }


    @Test
    void emailFailureThrows(){
        doThrow(new MailSendException("DOWN")).when(emailService).sendWorkOrderCreated(any());
        assertThrows(MailSendException.class, () -> notificationService.notifyOnce(workOrderCreatedEvent()));
    }
}
