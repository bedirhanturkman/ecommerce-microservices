package com.example.paymentservice.config;

import com.example.commonevents.inventory.InventoryReservedEvent;
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
    public ConsumerFactory<String, InventoryReservedEvent>
    inventoryReservedConsumerFactory() {

        Map<String, Object> properties =
                new HashMap<>();

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

        JacksonJsonDeserializer<InventoryReservedEvent>
                jsonDeserializer =
                new JacksonJsonDeserializer<>(
                        InventoryReservedEvent.class
                );

        jsonDeserializer.addTrustedPackages(
                "com.example.commonevents.inventory"
        );

        ErrorHandlingDeserializer<InventoryReservedEvent>
                valueDeserializer =
                new ErrorHandlingDeserializer<>(
                        jsonDeserializer
                );

        ErrorHandlingDeserializer<String>
                keyDeserializer =
                new ErrorHandlingDeserializer<>(
                        new StringDeserializer()
                );

        return new DefaultKafkaConsumerFactory<>(
                properties,
                keyDeserializer,
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory
            <String, InventoryReservedEvent>
    inventoryReservedKafkaListenerContainerFactory(
            PaymentKafkaErrorHandlerConfig
                    errorHandlerConfig
    ) {
        ConcurrentKafkaListenerContainerFactory
                <String, InventoryReservedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                inventoryReservedConsumerFactory()
        );

        factory.setCommonErrorHandler(
                errorHandlerConfig.createErrorHandler()
        );

        return factory;
    }
}