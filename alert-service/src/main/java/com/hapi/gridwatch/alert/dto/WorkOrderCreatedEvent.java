package com.hapi.gridwatch.alert.dto;

import java.time.Instant;

public record WorkOrderCreatedEvent(Long workOrderId, String substationId, String reason, Instant createdAt){
}
