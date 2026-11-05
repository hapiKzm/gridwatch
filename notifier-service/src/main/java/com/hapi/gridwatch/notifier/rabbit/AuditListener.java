package com.hapi.gridwatch.notifier.rabbit;

import com.hapi.gridwatch.notifier.dto.WorkOrderEvent;
import com.hapi.gridwatch.notifier.service.AuditService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuditListener {

    private final AuditService auditService;
    public AuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @RabbitListener(queues = "${rabbit.audit-queue}")
    public void receiveNotification(WorkOrderEvent workOrderEvent) {
        validateWorkOrderEvent(workOrderEvent);
        auditService.saveLog(workOrderEvent);
    }

    private void validateWorkOrderEvent(WorkOrderEvent workOrderEvent){
        if (!StringUtils.hasText(workOrderEvent.substationId())) throw new AmqpRejectAndDontRequeueException("substationId is required");
        if (!StringUtils.hasText(workOrderEvent.reason())) throw new AmqpRejectAndDontRequeueException("reason is required");
        if (workOrderEvent.workOrderId() == null) throw new AmqpRejectAndDontRequeueException("workOrderId is required");
        if (workOrderEvent.createdAt() == null) throw new AmqpRejectAndDontRequeueException("time is required");
        if (workOrderEvent.workOrderType() == null) throw new AmqpRejectAndDontRequeueException("status is required");
    }
}
