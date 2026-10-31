package com.hapi.gridwatch.sensor.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SensorSimulator {
    private static final Logger log = LoggerFactory.getLogger(SensorSimulator.class);
    private static final List<String> SUBSTATIONS = List.of("DEV-01", "DEV-02", "DEV-03");

    private final RestClient restClient;

    public SensorSimulator(@Value("${simulator.ingest-url}") String url,
                           @Value("${simulator.username}") String username,
                           @Value("${simulator.password}") String password) {
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeaders(h -> h.setBasicAuth(username, password))
                .build();
    }

    @Scheduled(fixedRateString = "${simulator.interval-ms}")
    public void sendReading() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String substation = SUBSTATIONS.get(random.nextInt(SUBSTATIONS.size()));
        boolean temperature = random.nextBoolean();
        String sensorType = temperature ? "TEMPERATURE" : "LOAD";
        double limit = temperature ? 90 : 100;
        double value = random.nextDouble() < 0.05
                ? limit + random.nextDouble(1, 30)
                : random.nextDouble(limit - 50, limit - 5);

        Map<String, Object> reading = Map.of(
                "substationId", substation,
                "sensorType", sensorType,
                "value", value,
                "measuredAt", Instant.now());

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reading)
                    .retrieve()
                    .toBodilessEntity();

        }catch (RestClientException e) {
            log.warn("Reading not sent: {}", e.getMessage());
        }
    }
}