package com.example.orderservice.config;

import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
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

    private static final String PAYMENT_PACKAGE =
            "com.example.commonevents.payment";

    private static final String INVENTORY_PACKAGE =
            "com.example.commonevents.inventory";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Bean
    public ConsumerFactory<String, PaymentSucceededEvent>
    paymentSucceededConsumerFactory() {

        return createConsumerFactory(
                PaymentSucceededEvent.class,
                PAYMENT_PACKAGE
        );
    }

    @Bean
    public ConsumerFactory<String, PaymentFailedEvent>
    paymentFailedConsumerFactory() {

        return createConsumerFactory(
                PaymentFailedEvent.class,
                PAYMENT_PACKAGE
        );
    }

    @Bean
    public ConsumerFactory
            <String, InventoryReservationFailedEvent>
    inventoryReservationFailedConsumerFactory() {

        return createConsumerFactory(
                InventoryReservationFailedEvent.class,
                INVENTORY_PACKAGE
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory
            <String, PaymentSucceededEvent>
    paymentSucceededKafkaListenerContainerFactory(
            OrderResultKafkaErrorHandlerConfig errorHandlerConfig
    ) {
        ConcurrentKafkaListenerContainerFactory
                <String, PaymentSucceededEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                paymentSucceededConsumerFactory()
        );

        factory.setCommonErrorHandler(
                errorHandlerConfig.createErrorHandler()
        );

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory
            <String, PaymentFailedEvent>
    paymentFailedKafkaListenerContainerFactory(
            OrderResultKafkaErrorHandlerConfig errorHandlerConfig
    ) {
        ConcurrentKafkaListenerContainerFactory
                <String, PaymentFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                paymentFailedConsumerFactory()
        );

        factory.setCommonErrorHandler(
                errorHandlerConfig.createErrorHandler()
        );

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory
            <String, InventoryReservationFailedEvent>
    inventoryReservationFailedKafkaListenerContainerFactory(
            OrderResultKafkaErrorHandlerConfig errorHandlerConfig
    ) {
        ConcurrentKafkaListenerContainerFactory
                <String, InventoryReservationFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                inventoryReservationFailedConsumerFactory()
        );

        factory.setCommonErrorHandler(
                errorHandlerConfig.createErrorHandler()
        );

        return factory;
    }

    private <T> ConsumerFactory<String, T>
    createConsumerFactory(
            Class<T> eventClass,
            String trustedPackage
    ) {
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

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        JacksonJsonDeserializer<T> jsonDeserializer =
                new JacksonJsonDeserializer<>(
                        eventClass
                );

        jsonDeserializer.setUseTypeHeaders(false);

        jsonDeserializer.addTrustedPackages(
                trustedPackage
        );

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new ErrorHandlingDeserializer<>(
                        new StringDeserializer()
                ),
                new ErrorHandlingDeserializer<>(
                        jsonDeserializer
                )
        );
    }
}