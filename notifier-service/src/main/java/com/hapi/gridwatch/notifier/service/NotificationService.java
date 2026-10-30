package com.hapi.gridwatch.notifier.service;


import com.hapi.gridwatch.notifier.dto.WorkOrderCreatedEvent;
import com.hapi.gridwatch.notifier.mail.EmailService;
import com.hapi.gridwatch.notifier.model.SentNotificationEntity;
import com.hapi.gridwatch.notifier.repository.SentNotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final SentNotificationRepository  sentNotificationRepository;
    private final EmailService emailService;
    private final MeterRegistry meterRegistry;
    public NotificationService(SentNotificationRepository sentNotificationRepository, EmailService emailService, MeterRegistry meterRegistry) {
        this.sentNotificationRepository = sentNotificationRepository;
        this.emailService = emailService;
        this.meterRegistry = meterRegistry;
    }


    @Transactional
    public void notifyOnce(WorkOrderCreatedEvent workOrderCreatedEvent) {
        boolean sendNotification = sentNotificationRepository.existsById(workOrderCreatedEvent.workOrderId());
        if (sendNotification) {
            log.info("Already notified for work order {}", workOrderCreatedEvent.workOrderId());
            return;
        }
        saveNotification(workOrderCreatedEvent);
        emailService.sendWorkOrderCreated(workOrderCreatedEvent);
        meterRegistry.counter("gridwatch.notifications.sent").increment();
    }

    private void saveNotification(WorkOrderCreatedEvent workOrderCreatedEvent) {

        SentNotificationEntity notification =
                new SentNotificationEntity(workOrderCreatedEvent.workOrderId());

        sentNotificationRepository.save(notification);
    }
}
