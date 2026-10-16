package com.hapi.gridwatch.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ReadingRestRequest(
        @NotNull
        UUID id,
        @NotBlank
        String substationId,
        @NotBlank
        String sensorType,
        @NotNull
        Double value,
        @NotNull
        Instant measuredAt
) { }
