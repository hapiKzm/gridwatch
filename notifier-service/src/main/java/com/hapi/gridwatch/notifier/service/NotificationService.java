package com.hapi.gridwatch.notifier.service;


import com.hapi.gridwatch.notifier.dto.WorkOrderEvent;
import com.hapi.gridwatch.notifier.mail.EmailService;
import com.hapi.gridwatch.notifier.model.SentNotificationEntity;
import com.hapi.gridwatch.notifier.model.SentNotificationPkEntity;
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
    public void notifyOnce(WorkOrderEvent workOrderEvent) {

        SentNotificationPkEntity sentNotificationPkEntity = new SentNotificationPkEntity(
                workOrderEvent.workOrderId(),
                workOrderEvent.workOrderType()
        );
        boolean sendNotification =
                sentNotificationRepository.existsById(sentNotificationPkEntity);
        if (sendNotification) {
            log.info("Already notified for work order {}", workOrderEvent.workOrderId());
            return;
        }
        saveNotification(sentNotificationPkEntity);
        emailService.sendWorkOrder(workOrderEvent);
        meterRegistry.counter("gridwatch.notifications.sent").increment();
    }

    private void saveNotification(SentNotificationPkEntity id) {
        SentNotificationEntity notification = new SentNotificationEntity();
        notification.setId(id);
        sentNotificationRepository.save(notification);
    }
}
