package com.hapi.gridwatch.alert.service;

import com.hapi.gridwatch.alert.dto.ReadingRestRequest;
import com.hapi.gridwatch.alert.dto.WorkOrderCreatedEvent;
import com.hapi.gridwatch.alert.exception.InvalidReadingException;
import com.hapi.gridwatch.alert.repository.ReadingRepository;
import com.hapi.gridwatch.alert.repository.WorkOrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ReadingServiceTest {

    @Mock
    ReadingRepository readingRepository;
    @Mock
    WorkOrderRepository workOrderRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ReadingService readingService;

    @Spy
    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ReadingRestRequest reading(String sensorType, Double value){
        return new ReadingRestRequest(
                UUID.randomUUID(),
                "DEV-01",
                sensorType,
                value,
                Instant.now()
        );
    }

    @Test
    void temperatureAboveLimitCreatesWorkOrder() {
        when(workOrderRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        readingService.saveReadingRequest(reading("TEMPERATURE", 95.0D));
        verify(readingRepository).save(any());
        verify(workOrderRepository).save(any());
        verify(eventPublisher).publishEvent(any(WorkOrderCreatedEvent.class));
    }

    @Test
    void valueAtLimitCreatesNoWorkOrder(){
        readingService.saveReadingRequest(reading("TEMPERATURE", 90.0D));
        verify(readingRepository).save(any());
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderCreatedEvent.class));
    }

    @Test
    void duplicateReadingIsSkipped(){
        when(readingRepository.existsById(any())).thenReturn(true);
        readingService.saveReadingRequest(reading("TEMPERATURE", 90.0D));
    verify(readingRepository, never()).save(any());
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderCreatedEvent.class));
    }

    @Test
    void unknownSensorTypeIsRejected(){
        assertThrows(InvalidReadingException.class, () -> readingService.saveReadingRequest(reading("Unknown", 90.0D)));
        verify(readingRepository, never()).save(any());
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderCreatedEvent.class));
    }
    @Test
    void missingValueIsRejected(){
        assertThrows(InvalidReadingException.class, () -> readingService.saveReadingRequest(reading("TEMPERATURE", null)));
        verify(readingRepository, never()).save(any());
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderCreatedEvent.class));
    }
}

