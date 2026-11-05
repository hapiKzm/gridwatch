package com.hapi.gridwatch.notifier.service;

import com.hapi.gridwatch.notifier.dto.WorkOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void saveLog(WorkOrderEvent workOrderEvent) {
        log.info("LOG event: {}, type: {}, time:{}",
                workOrderEvent.workOrderId(),
                workOrderEvent.workOrderType(),
                Instant.now()
        );
    }
}
