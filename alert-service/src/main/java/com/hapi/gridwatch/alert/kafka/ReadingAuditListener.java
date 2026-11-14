package com.hapi.gridwatch.alert.kafka;

import com.hapi.gridwatch.alert.dto.ReadingRestRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ReadingAuditListener {

    private final static Logger logger = LoggerFactory.getLogger(ReadingAuditListener.class);

    @KafkaListener(topics = "${kafka-topic.reading}", groupId = "${kafka-topic.audit-group}" )
    public void onReading(ConsumerRecord<String, ReadingRestRequest> record) {

        logger.info(
                "Received Reading for record {}, partition {}, offset {}",
                record.value().id(),
                record.partition(),
                record.offset()
        );
    }
}
