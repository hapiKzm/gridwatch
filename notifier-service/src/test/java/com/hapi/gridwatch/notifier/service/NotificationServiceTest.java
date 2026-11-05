package com.hapi.gridwatch.notifier.service;


import com.hapi.gridwatch.notifier.dto.WorkOrderEvent;
import com.hapi.gridwatch.notifier.mail.EmailService;
import com.hapi.gridwatch.notifier.model.SentNotificationPkEntity;
import com.hapi.gridwatch.notifier.repository.SentNotificationRepository;
import com.hapi.gridwatch.notifier.types.WorkOrderType;
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

    private WorkOrderEvent workOrderEvent(WorkOrderType workOrderType){
        return new WorkOrderEvent(
                1L,
                "DEV-01",
                "LOAD 130 above limit 100",
                Instant.now(),
                workOrderType
        );
    }

    private SentNotificationPkEntity createSentNotificationPkEntity(Long workOrderId, WorkOrderType workOrderType){
        return new SentNotificationPkEntity(
                workOrderId,
                workOrderType
        );
    }

    @Test
    void newWorkOrderIsSavedAndEmailed(){
        WorkOrderEvent workOrderEvent = workOrderEvent(WorkOrderType.OPEN);
        notificationService.notifyOnce(workOrderEvent);
        verify(sentNotificationRepository).save(any());
        verify(emailService).sendWorkOrder(workOrderEvent);
    }

    @Test
    void alreadyNotifiedSendsNoEmail(){
        SentNotificationPkEntity id = createSentNotificationPkEntity(1L, WorkOrderType.OPEN);
        when(sentNotificationRepository.existsById(id)).thenReturn(true);

        WorkOrderEvent workOrderEvent = workOrderEvent(WorkOrderType.OPEN);
        notificationService.notifyOnce(workOrderEvent);
        verify(sentNotificationRepository, never()).save(any());
        verify(emailService, never()).sendWorkOrder(any());
    }

    @Test
    void closedIsEmailedEvenWhenCreatedWasAlreadySent(){
        SentNotificationPkEntity closedKey = createSentNotificationPkEntity(1L, WorkOrderType.OPEN);
        lenient().when(sentNotificationRepository.existsById(closedKey)).thenReturn(true);

        WorkOrderEvent closed = workOrderEvent(WorkOrderType.CLOSED);
        notificationService.notifyOnce(closed);

        verify(sentNotificationRepository).save(any());
        verify(emailService).sendWorkOrder(closed);
    }

    @Test
    void alreadyClosedSendsNoEmail(){
        SentNotificationPkEntity id = createSentNotificationPkEntity(1L, WorkOrderType.CLOSED);
        when(sentNotificationRepository.existsById(id)).thenReturn(true);

        WorkOrderEvent workOrderEvent = workOrderEvent(WorkOrderType.CLOSED);
        notificationService.notifyOnce(workOrderEvent);
        verify(sentNotificationRepository, never()).save(any());
        verify(emailService, never()).sendWorkOrder(any());
    }

    @Test
    void emailFailureThrows(){
        doThrow(new MailSendException("DOWN")).when(emailService).sendWorkOrder(any());
        assertThrows(MailSendException.class, () -> notificationService.notifyOnce(workOrderEvent(WorkOrderType.OPEN)));
    }

}
