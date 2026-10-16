package com.hapi.gridwatch.alert.dto;

import com.hapi.gridwatch.alert.enums.WorkOrderStatusOption;

import java.time.Instant;

public record WorkOrderDto(
        Long id,
        String substationId,
        WorkOrderStatusOption workOrderStatusOption,
        String reason,
        Instant createdAt
){ }
