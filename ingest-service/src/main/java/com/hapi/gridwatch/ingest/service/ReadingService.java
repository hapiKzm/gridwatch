package com.hapi.gridwatch.ingest.service;


import com.hapi.gridwatch.ingest.config.AlertServiceProperties;
import com.hapi.gridwatch.ingest.dto.UserReadingRequest;
import com.hapi.gridwatch.ingest.rest.ReadingRestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class ReadingService {

    private static final Logger log = LoggerFactory.getLogger(ReadingService.class);
    private final RestClient alertClient;

    public ReadingService(AlertServiceProperties alert) {
        this.alertClient = RestClient.builder()
                .baseUrl(alert.url())
                .requestInterceptor(new BasicAuthenticationInterceptor(alert.username(), alert.password()))
                .build();
    }

    public UUID forward(UserReadingRequest in) {
        UUID id = UUID.randomUUID();
        ReadingRestRequest out = new ReadingRestRequest(
                id,
                in.substationId(),
                in.sensorType(),
                in.value(),
                in.measuredAt()
        );

        alertClient.post()
                .uri("/reading/internal")
                .contentType(MediaType.APPLICATION_JSON)
                .body(out)
                .retrieve()
                .toBodilessEntity();
        log.info("Forwarded reading {} for {}", id, in.substationId());
        return id;
    }
}
