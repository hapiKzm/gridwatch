package com.hapi.gridwatch.ingest.service;

import com.hapi.gridwatch.ingest.dto.UserReadingRequest;
import com.hapi.gridwatch.ingest.exception.InvalidReadingException;
import com.hapi.gridwatch.ingest.exception.KafkaUnavailableException;
import com.hapi.gridwatch.ingest.rest.ReadingRestRequest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingServiceTest {

    @Mock
    KafkaTemplate<String, ReadingRestRequest> kafka;


    ReadingService readingService;

    @BeforeEach
    void setup() {
        readingService = new ReadingService(kafka, "gridwatch.readings", new SimpleMeterRegistry());
    }

    private UserReadingRequest request(String sensorType) {
        return new UserReadingRequest("DEV-01", sensorType, 95.0, Instant.now());
    }

    @Test
    @SuppressWarnings("unchecked")
    void validReadingIsPublishedWithSubstationKey() {
        SendResult<String, ReadingRestRequest> result = mock(SendResult.class, RETURNS_DEEP_STUBS);
        when(kafka.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(result));

        UUID id = readingService.publish(request("TEMPERATURE"));

        assertThat(id).isNotNull();
        verify(kafka).send(eq("gridwatch.readings"), eq("DEV-01"), any(ReadingRestRequest.class));
    }

    @Test
    void unknownSensorTypeIsRejected() {
        assertThrows(InvalidReadingException.class, () -> readingService.publish(request("WIND")));
        verify(kafka, never()).send(any(), any(), any());
    }

    @Test
    void kafkaDownThrowsKafkaUnavailable() {
        when(kafka.send(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new KafkaException("down")));

        assertThrows(KafkaUnavailableException.class, () -> readingService.publish(request("TEMPERATURE")));
    }
}
