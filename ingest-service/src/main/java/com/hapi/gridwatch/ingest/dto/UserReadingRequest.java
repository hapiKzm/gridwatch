package com.hapi.gridwatch.ingest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UserReadingRequest(
        @NotBlank
        String substationId,
        @NotBlank
        String sensorType,
        @NotNull
        Double value,
        @NotNull
        Instant measuredAt
) {}
