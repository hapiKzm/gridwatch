package com.hapi.gridwatch.alert.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Topics {

    @Bean
    public NewTopic readingDLTTopic(@Value("${kafka-topic.reading-dlt}") String topic){
        return new NewTopic(topic, 3, (short) 1);
    };
}
