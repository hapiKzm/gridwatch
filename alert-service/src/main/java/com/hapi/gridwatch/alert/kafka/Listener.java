package com.hapi.gridwatch.alert.kafka;

import com.hapi.gridwatch.alert.dto.ReadingRestRequest;
import com.hapi.gridwatch.alert.service.ReadingService;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class Listener {

    private final ReadingService readingService;
    public Listener(ReadingService readingService) {
        this.readingService = readingService;
    }

    @KafkaListener( topics = "gridwatch.readings", groupId = "alert.gridwatch" )
    public void onReading(ReadingRestRequest readingRestRequest, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        readingService.saveReading(readingRestRequest);
    }
}
