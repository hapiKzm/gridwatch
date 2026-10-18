package com.hapi.gridwatch.alert.kafka;

import com.hapi.gridwatch.alert.exception.UnknownSensorTypeException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;


@Configuration
public class KafkaErrorHandler {


    @Bean
    KafkaTemplate<Object, Object> dltTemplate(ProducerFactory<Object, Object> producerFactory) {
        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());              // broken messages: original bytes
        delegates.put(Object.class, new JacksonJsonSerializer<>()); // readings that failed in saveReading
        var valueSerializer = new DelegatingByTypeSerializer(delegates, true);

        var factory = new DefaultKafkaProducerFactory<Object, Object>(
                producerFactory.getConfigurationProperties(), null, valueSerializer);
        return new KafkaTemplate<>(factory);
    }
    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        var recover = new DeadLetterPublishingRecoverer(template);
        var errorHandler = new DefaultErrorHandler(recover, new FixedBackOff(1000L, 3)); // 3 retries, 1 s apart
        errorHandler.addNotRetryableExceptions(UnknownSensorTypeException.class);
        return errorHandler;
    }
}
