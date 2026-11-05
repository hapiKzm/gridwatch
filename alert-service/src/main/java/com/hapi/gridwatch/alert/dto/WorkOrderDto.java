package com.hapi.gridwatch.alert.dto;

import com.hapi.gridwatch.alert.enums.WorkOrderType;

import java.time.Instant;

public record WorkOrderDto(
        Long id,
        String substationId,
        WorkOrderType workOrderType,
        String reason,
        Instant createdAt
){ }
