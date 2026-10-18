package com.hapi.gridwatch.alert.kafka;

import com.hapi.gridwatch.alert.dto.ReadingRestRequest;
import com.hapi.gridwatch.alert.service.ReadingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class Listener {

    private final ReadingService readingService;
    public Listener(ReadingService readingService) {
        this.readingService = readingService;
    }

    @KafkaListener(topics = "${kafka-topic.reading}", groupId = "${spring.kafka.consumer.group-id}" )
    public void onReading(ReadingRestRequest readingRestRequest) {
        readingService.saveReadingRequest(readingRestRequest);
    }
}
