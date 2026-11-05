package com.hapi.gridwatch.notifier.rabbit;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public TopicExchange workOrderExchange(@Value("${rabbit.exchange}") String exchange){
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue notifierQueue(@Value("${rabbit.queue}") String queueName, @Value("${rabbit.dlq}") String notifierDlq ){
        return QueueBuilder.durable(queueName)
                .deadLetterExchange("")
                .deadLetterRoutingKey(notifierDlq)
                .build();
    }

    @Bean
    public Queue notifierDlq(@Value("${rabbit.dlq}") String queueName){
        return QueueBuilder.durable(queueName)
                .build();
    }

    @Bean
    public Binding notifierBinding(Queue notifierQueue, TopicExchange workOrderExchange, @Value("${rabbit.binding-key}") String bindingKey){
        return BindingBuilder
                .bind(notifierQueue)
                .to(workOrderExchange)
                .with(bindingKey);
    }

    @Bean
    public Queue auditQueue(@Value("${rabbit.audit-queue}") String queueName, @Value("${rabbit.audit-dlq}") String auditDlq ){
        return QueueBuilder.durable(queueName)
                .deadLetterExchange("")
                .deadLetterRoutingKey(auditDlq)
                .build();
    }

    @Bean
    public Queue auditDlq(@Value("${rabbit.audit-dlq}") String queueName){
        return QueueBuilder.durable(queueName)
                .build();
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange workOrderExchange, @Value("${rabbit.audit-binding-key}") String bindingKey){
        return BindingBuilder
                .bind(auditQueue)
                .to(workOrderExchange)
                .with(bindingKey);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter(){
        return new JacksonJsonMessageConverter();
    }


    @Bean
    public RabbitListenerRetrySettingsCustomizer skipRetryForInvalidMessages(){
        return setting -> setting.setExceptionPredicate(
                e -> !(
                        e instanceof AmqpRejectAndDontRequeueException ||
                                e.getCause()  instanceof AmqpRejectAndDontRequeueException
                )
        );
    }
}
