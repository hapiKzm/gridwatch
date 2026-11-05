package com.hapi.gridwatch.notifier.rabbit;


import com.hapi.gridwatch.notifier.dto.WorkOrderEvent;
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
    public void receiveWorkOrderEvent(WorkOrderEvent workOrderEvent){
        validateWorkOrderEvent(workOrderEvent);
        notificationService.notifyOnce(workOrderEvent);
    }

    private void validateWorkOrderEvent(WorkOrderEvent workOrderEvent){
        if (!StringUtils.hasText(workOrderEvent.substationId())) throw new AmqpRejectAndDontRequeueException("substationId is required");
        if (!StringUtils.hasText(workOrderEvent.reason())) throw new AmqpRejectAndDontRequeueException("reason is required");
        if (workOrderEvent.workOrderId() == null) throw new AmqpRejectAndDontRequeueException("workOrderId is required");
        if (workOrderEvent.createdAt() == null) throw new AmqpRejectAndDontRequeueException("time is required");
        if (workOrderEvent.workOrderType() == null) throw new AmqpRejectAndDontRequeueException("status is required");
    }
}
