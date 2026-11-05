package com.hapi.gridwatch.alert.dto;

import com.hapi.gridwatch.alert.enums.WorkOrderType;

import java.time.Instant;

public record WorkOrderEvent(Long workOrderId, String substationId, String reason, Instant createdAt, WorkOrderType workOrderType){}
