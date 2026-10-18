package com.hapi.gridwatch.ingest.rest;

import java.time.Instant;
import java.util.UUID;

public record ReadingRestRequest(
    UUID id,
    String substationId,
    String sensorType,
    Double value,
    Instant measuredAt
){}
