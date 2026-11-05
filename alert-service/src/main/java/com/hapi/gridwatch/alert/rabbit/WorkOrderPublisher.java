package com.hapi.gridwatch.alert.rabbit;

import com.hapi.gridwatch.alert.dto.WorkOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;

@Component
public class WorkOrderPublisher {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderPublisher.class);
    private final RabbitTemplate rabbit;

    private final String exchange;
    private final String routingKeyPrefix;

    public WorkOrderPublisher(RabbitTemplate rabbit,
                              @Value("${rabbit.exchange}") String exchange,
                              @Value("${rabbit.routing-key-prefix}") String routingKeyPrefix
    ) {
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.routingKeyPrefix = routingKeyPrefix;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void workOrderEvent(WorkOrderEvent event) {
        String routingKey = routingKeyPrefix + "." + event.workOrderType().name().toLowerCase(Locale.ROOT);
        rabbit.convertAndSend(exchange, routingKey, event);
        log.info("Published work order {} to RabbitMQ with key {}", event.workOrderId(), routingKey);
    }
}
