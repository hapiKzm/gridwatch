package com.hapi.gridwatch.alert.service;

import com.hapi.gridwatch.alert.dto.ReadingDto;
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
    public void saveReading(ReadingDto readingDto) throws Exception {
        if (readingDto.id() != null && readingRepository.existsById(readingDto.id())) return;

        ReadingEntity readingEntity = new ReadingEntity(
                readingDto.id(),
                readingDto.substationId(),
                readingDto.sensorType(),
                readingDto.value(),
                readingDto.measuredAt()
        );

        readingRepository.save(readingEntity);

        if (readingDto.value() > readingDto.sensorType().limit)
            saveWorkOrder(readingEntity);

    }

    private void saveWorkOrder(ReadingEntity readingEntity) {
        wordOrderRepository.save(
                new WorkOrderEntity(
                        readingEntity,
                        WorkOrderStatusOption.OPEN,
                        readingEntity.getSensorType().name + " " +
                                readingEntity.getValue() + " " +
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
