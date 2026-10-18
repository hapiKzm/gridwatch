package com.hapi.gridwatch.alert.rabbit;

import com.hapi.gridwatch.alert.dto.WorkOrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkOrderPublisher {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderPublisher.class);
    private final RabbitTemplate rabbit;

    private final String exchange;
    private final String routingKey;

    public WorkOrderPublisher(RabbitTemplate rabbit,
                              @Value("${rabbit.exchange}") String exchange,
                              @Value("${rabbit.routing-key}") String routingKey
    ) {
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void workOrderCreated(WorkOrderCreatedEvent event) {
        rabbit.convertAndSend(exchange, routingKey, event);
        log.info("Published work order {} to RabbitMQ", event.workOrderId());

    }
}
