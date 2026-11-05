package com.hapi.gridwatch.notifier.dto;

import com.hapi.gridwatch.notifier.types.WorkOrderType;

import java.time.Instant;

public record WorkOrderEvent(Long workOrderId, String substationId, String reason, Instant createdAt, WorkOrderType workOrderType){
}
