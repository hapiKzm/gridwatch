package com.hapi.gridwatch.alert.service;

import com.hapi.gridwatch.alert.dto.ReadingRestRequest;
import com.hapi.gridwatch.alert.dto.WorkOrderDto;
import com.hapi.gridwatch.alert.enums.SensorTypeOption;
import com.hapi.gridwatch.alert.enums.WorkOrderStatusOption;
import com.hapi.gridwatch.alert.model.ReadingEntity;
import com.hapi.gridwatch.alert.model.WorkOrderEntity;
import com.hapi.gridwatch.alert.repository.ReadingRepository;
import com.hapi.gridwatch.alert.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReadingService {

    private final ReadingRepository readingRepository;
    private final WorkOrderRepository wordOrderRepository;

    public ReadingService(ReadingRepository readingRepository, WorkOrderRepository wordOrderRepository) {
        this.readingRepository = readingRepository;
        this.wordOrderRepository = wordOrderRepository;
    }



    @Transactional( isolation = Isolation.DEFAULT)
    public void saveReading(ReadingRestRequest readingRestRequest) throws Exception {
        if (readingRestRequest.id() != null && readingRepository.existsById(readingRestRequest.id())) return;

        SensorTypeOption sensorTypeOption =
                SensorTypeOption.findByName(readingRestRequest.sensorType());

        if (sensorTypeOption == null) throw new Exception("SensorType not found");

        ReadingEntity readingEntity = new ReadingEntity(
                readingRestRequest.id(),
                readingRestRequest.substationId(),
                SensorTypeOption.findByName(readingRestRequest.sensorType()),
                readingRestRequest.value(),
                readingRestRequest.measuredAt()
        );

        readingRepository.save(readingEntity);

        if (readingRestRequest.value() > sensorTypeOption.limit)
            saveWorkOrder(readingEntity);

    }

    private void saveWorkOrder(ReadingEntity readingEntity) {
        wordOrderRepository.save(
                new WorkOrderEntity(
                        readingEntity,
                        WorkOrderStatusOption.OPEN,
                        readingEntity.getSensorType().name + " " +
                                readingEntity.getValue() + " above limit " +
                                readingEntity.getSensorType().limit
                )
        );
    }

    public List<WorkOrderDto> getAllWorkOrder() {
        return wordOrderRepository.findAll()
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
}
