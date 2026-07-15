package com.example.inventoryservice.config;

import com.example.commonevents.order.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent>
    orderCreatedConsumerFactory() {

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                autoOffsetReset
        );

        JacksonJsonDeserializer<OrderCreatedEvent> jsonDeserializer =
                new JacksonJsonDeserializer<>(
                        OrderCreatedEvent.class
                );

        jsonDeserializer.addTrustedPackages(
                "com.example.commonevents.order"
        );

        ErrorHandlingDeserializer<OrderCreatedEvent>
                errorHandlingValueDeserializer =
                new ErrorHandlingDeserializer<>(
                        jsonDeserializer
                );

        ErrorHandlingDeserializer<String>
                errorHandlingKeyDeserializer =
                new ErrorHandlingDeserializer<>(
                        new StringDeserializer()
                );

        return new DefaultKafkaConsumerFactory<>(
                properties,
                errorHandlingKeyDeserializer,
                errorHandlingValueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory
            <String, OrderCreatedEvent>
    orderCreatedKafkaListenerContainerFactory(
            KafkaErrorHandlerConfig kafkaErrorHandlerConfig
    ) {
        ConcurrentKafkaListenerContainerFactory
                <String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                orderCreatedConsumerFactory()
        );

        factory.setCommonErrorHandler(
                kafkaErrorHandlerConfig.createErrorHandler()
        );
        return factory;
    }
}