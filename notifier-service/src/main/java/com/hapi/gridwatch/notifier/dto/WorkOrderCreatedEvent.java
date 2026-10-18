package com.hapi.gridwatch.notifier.dto;

import java.time.Instant;

public record WorkOrderCreatedEvent(Long workOrderId, String substationId, String reason, Instant createdAt){
}
