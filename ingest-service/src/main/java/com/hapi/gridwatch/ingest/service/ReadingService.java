package com.hapi.gridwatch.ingest.service;

import com.hapi.gridwatch.ingest.Exception.KafkaUnavailableException;
import com.hapi.gridwatch.ingest.dto.UserReadingRequest;
import com.hapi.gridwatch.ingest.enums.SensorTypeOption;
import com.hapi.gridwatch.ingest.rest.ReadingRestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ReadingService {
    private static final Logger log = LoggerFactory.getLogger(ReadingService.class);
    private final KafkaTemplate<String, ReadingRestRequest> kafka;
    private final String topic;

    public ReadingService(
            KafkaTemplate<String, ReadingRestRequest> kafka,
            @Value("${kafka-topic.reading}") String topic
    ) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public UUID publish(UserReadingRequest in) {
        UUID id = UUID.randomUUID();

        try {
            SensorTypeOption.valueOf(in.sensorType());
        }catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown sensor type: " + in.sensorType());
        }

        ReadingRestRequest out = new ReadingRestRequest(
                id,
                in.substationId(),
                in.sensorType(),
                in.value(),
                in.measuredAt()
        );

        try {
            var result = kafka.send(topic, in.substationId(), out).get(5, TimeUnit.SECONDS);
            log.info("Published {} to partition {} offset {}",
                    id,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
            return id;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaUnavailableException("Interrupted while publishing", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new KafkaUnavailableException("Kafka not reachable", e);
        }
    }
}
