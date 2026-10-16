package com.hapi.gridwatch.alert.dto;

import com.hapi.gridwatch.alert.enums.SensorTypeOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ReadingDto(
        @NotNull
        UUID id,
        @NotBlank
        String substationId,
        @NotNull
        SensorTypeOption sensorType,
        @NotNull
        Double value,
        @NotNull
        Instant measuredAt
) { }
