package com.hapi.gridwatch.alert.service;

import com.hapi.gridwatch.alert.dto.WorkOrderEvent;
import com.hapi.gridwatch.alert.exception.InvalidReadingException;
import com.hapi.gridwatch.alert.dto.ReadingRestRequest;
import com.hapi.gridwatch.alert.dto.WorkOrderDto;
import com.hapi.gridwatch.alert.enums.SensorTypeOption;
import com.hapi.gridwatch.alert.enums.WorkOrderType;
import com.hapi.gridwatch.alert.exception.WorkOrderNotFoundException;
import com.hapi.gridwatch.alert.model.ReadingEntity;
import com.hapi.gridwatch.alert.model.WorkOrderEntity;
import com.hapi.gridwatch.alert.repository.ReadingRepository;
import com.hapi.gridwatch.alert.repository.WorkOrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReadingService {

    private final ReadingRepository readingRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ApplicationEventPublisher workOrderPublisher;
    private final MeterRegistry meterRegistry;

    public ReadingService(ReadingRepository readingRepository, WorkOrderRepository workOrderRepository, ApplicationEventPublisher workOrderPublisher, MeterRegistry meterRegistry) {
        this.readingRepository = readingRepository;
        this.workOrderRepository = workOrderRepository;
        this.workOrderPublisher = workOrderPublisher;
        this.meterRegistry = meterRegistry;
    }



    @Transactional
    public void saveReadingRequest(ReadingRestRequest readingRestRequest){
        validateReading(readingRestRequest);
        if (readingRestRequest.id() != null && readingRepository.existsById(readingRestRequest.id())) return;

        saveReading(readingRestRequest);

    }

    private void saveReading(ReadingRestRequest readingRestRequest) {
        SensorTypeOption sensorTypeOption = null;
        try {
            sensorTypeOption =
                    SensorTypeOption.valueOf(readingRestRequest.sensorType());
        }catch (IllegalArgumentException  e) {
            throw new InvalidReadingException("Unknown sensor type: " + readingRestRequest.sensorType());
        }
        ReadingEntity readingEntity = new ReadingEntity(
                readingRestRequest.id(),
                readingRestRequest.substationId(),
                sensorTypeOption,
                readingRestRequest.value(),
                readingRestRequest.measuredAt()
        );

        readingRepository.save(readingEntity);

        if (readingRestRequest.value() > sensorTypeOption.limit)
            saveWorkOrder(readingEntity);
    }

    private void validateReading(ReadingRestRequest readingRestRequest) {
        if (readingRestRequest.value() == null) throw new InvalidReadingException("value is null") ;
        if (!StringUtils.hasText(readingRestRequest.sensorType())) throw new InvalidReadingException("sensor type is null") ;
        if (!StringUtils.hasText(readingRestRequest.substationId())) throw new InvalidReadingException("substationId is null") ;
        if (readingRestRequest.measuredAt() == null) throw new InvalidReadingException("measuredAt is null") ;
    }

    private void saveWorkOrder(ReadingEntity readingEntity) {
        WorkOrderEntity workOrder = workOrderRepository.save(
                new WorkOrderEntity(
                        readingEntity,
                        WorkOrderType.OPEN,
                        readingEntity.getSensorType() + " " +
                                readingEntity.getValue() + " above limit " +
                                readingEntity.getSensorType().limit
                )
        );

        meterRegistry.counter(
                "gridwatch.work_orders.created",
                "sensor",
                readingEntity.getSensorType().name()
        ).increment();

        // send to rabbitmq
        workOrderPublisher.publishEvent(
                new WorkOrderEvent(
                        workOrder.getId(),
                        workOrder.getSubstationId(),
                        workOrder.getReason(),
                        workOrder.getCreatedAt(),
                        WorkOrderType.OPEN
                )
        );
    }

    public List<WorkOrderDto> getAllWorkOrder() {
        return workOrderRepository.findAll()
                .stream()
                .map( m -> new WorkOrderDto(
                        m.getId(),
                        m.getSubstationId(),
                        m.getWorkOrderStatus(),
                        m.getReason(),
                        m.getCreatedAt()
                        )
                ).collect(Collectors.toList());
    }

    @Transactional
    public void closeWorkOrder(Long workOrderId){
        WorkOrderEntity wo =
                workOrderRepository.findById(workOrderId).orElseThrow(
                        () -> new WorkOrderNotFoundException("WORK ORDER NOT FOUND")
                );

        if (wo.getClosedAt() != null) return;

        wo.setClosedAt(Instant.now());
        wo.setWorkOrderStatus(WorkOrderType.CLOSED);
        workOrderRepository.save(wo);

        workOrderPublisher.publishEvent(
                new WorkOrderEvent(
                        wo.getId(),
                        wo.getSubstationId(),
                        wo.getReason(),
                        wo.getCreatedAt(),
                        WorkOrderType.CLOSED
                )
        );
    }
}
