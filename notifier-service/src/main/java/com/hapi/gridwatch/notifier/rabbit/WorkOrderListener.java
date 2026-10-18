package com.hapi.gridwatch.notifier.rabbit;


import com.hapi.gridwatch.notifier.dto.WorkOrderCreatedEvent;
import com.hapi.gridwatch.notifier.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WorkOrderListener {

    private final static Logger logger = LoggerFactory.getLogger(WorkOrderListener.class);

    private final NotificationService notificationService;

    public WorkOrderListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${rabbit.queue}")
    public void receiveWorkOrderCreatedEvent(WorkOrderCreatedEvent workOrderCreatedEvent){
        validateWorkOrderCreatedEvent(workOrderCreatedEvent);
        notificationService.notifyOnce(workOrderCreatedEvent);
    }

    private void validateWorkOrderCreatedEvent(WorkOrderCreatedEvent workOrderCreatedEvent){
        if (!StringUtils.hasText(workOrderCreatedEvent.substationId())) throw new AmqpRejectAndDontRequeueException("substationId is required");
        if (!StringUtils.hasText(workOrderCreatedEvent.reason())) throw new AmqpRejectAndDontRequeueException("reason is required");
        if (workOrderCreatedEvent.workOrderId() == null) throw new AmqpRejectAndDontRequeueException("workOrderId is required");
        if (workOrderCreatedEvent.createdAt() == null) throw new AmqpRejectAndDontRequeueException("time is required");
    }
}
