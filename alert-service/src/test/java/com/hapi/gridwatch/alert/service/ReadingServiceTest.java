package com.hapi.gridwatch.alert.service;

import com.hapi.gridwatch.alert.dto.ReadingRestRequest;
import com.hapi.gridwatch.alert.dto.WorkOrderEvent;
import com.hapi.gridwatch.alert.enums.WorkOrderType;
import com.hapi.gridwatch.alert.exception.InvalidReadingException;
import com.hapi.gridwatch.alert.exception.WorkOrderNotFoundException;
import com.hapi.gridwatch.alert.model.ReadingEntity;
import com.hapi.gridwatch.alert.model.WorkOrderEntity;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
        verify(eventPublisher).publishEvent(any(WorkOrderEvent.class));
    }

    @Test
    void valueAtLimitCreatesNoWorkOrder(){
        readingService.saveReadingRequest(reading("TEMPERATURE", 90.0D));
        verify(readingRepository).save(any());
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderEvent.class));
    }

    @Test
    void duplicateReadingIsSkipped(){
        when(readingRepository.existsById(any())).thenReturn(true);
        readingService.saveReadingRequest(reading("TEMPERATURE", 90.0D));
    verify(readingRepository, never()).save(any());
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderEvent.class));
    }

    @Test
    void unknownSensorTypeIsRejected(){
        assertThrows(InvalidReadingException.class, () -> readingService.saveReadingRequest(reading("Unknown", 90.0D)));
        verify(readingRepository, never()).save(any());
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderEvent.class));
    }

    @Test
    void missingValueIsRejected(){
        assertThrows(InvalidReadingException.class, () -> readingService.saveReadingRequest(reading("TEMPERATURE", null)));
        verify(readingRepository, never()).save(any());
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderEvent.class));
    }

    @Test
    void closeSetsClosedAtAndPublishes() {

        WorkOrderEntity workOrder = new WorkOrderEntity(
                new ReadingEntity(),
                WorkOrderType.OPEN,
                ""
        );
        when(workOrderRepository.findById(42L)).thenReturn(Optional.of(workOrder));
        readingService.closeWorkOrder(42L);
        assertNotNull(workOrder.getClosedAt());
        assertEquals(WorkOrderType.CLOSED, workOrder.getWorkOrderStatus());
        verify(workOrderRepository).save(any());
        verify(eventPublisher).publishEvent(any(WorkOrderEvent.class));
    }
   @Test
    void closeTwiceDoesNothing() {
        WorkOrderEntity workOrder = new WorkOrderEntity(
                new ReadingEntity(),
                WorkOrderType.CLOSED,
                ""
        );
       Instant now = Instant.now();
       workOrder.setClosedAt(now);
        when(workOrderRepository.findById(42L)).thenReturn(Optional.of(workOrder));
        readingService.closeWorkOrder(42L);
        assertEquals(now, workOrder.getClosedAt());
        assertEquals(WorkOrderType.CLOSED, workOrder.getWorkOrderStatus());
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderEvent.class));
    }

    @Test
    void closeUnknownIdThrows() {
        when(workOrderRepository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(WorkOrderNotFoundException.class, () -> readingService.closeWorkOrder(42L));
        verify(workOrderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(WorkOrderEvent.class));
    }



}

