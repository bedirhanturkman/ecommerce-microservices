package com.example.paymentservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${payment.kafka.producer.max-block-ms}")
    private long maxBlockMs;

    @Value("${payment.kafka.producer.request-timeout-ms}")
    private int requestTimeoutMs;

    @Value("${payment.kafka.producer.delivery-timeout-ms}")
    private int deliveryTimeoutMs;

    @Bean
    public ProducerFactory<String, Object>
    paymentKafkaProducerFactory() {

        Map<String, Object> properties =
                new LinkedHashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ProducerConfig.MAX_BLOCK_MS_CONFIG,
                maxBlockMs
        );

        properties.put(
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                requestTimeoutMs
        );

        properties.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                deliveryTimeoutMs
        );

        properties.put(
                ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG,
                500
        );

        properties.put(
                ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG,
                1000
        );

        Map<Class<?>, Serializer<?>> serializers =
                new LinkedHashMap<>();

        serializers.put(
                byte[].class,
                new ByteArraySerializer()
        );

        serializers.put(
                Object.class,
                new JacksonJsonSerializer<>()
        );

        DelegatingByTypeSerializer valueSerializer =
                new DelegatingByTypeSerializer(
                        serializers,
                        true
                );

        return new DefaultKafkaProducerFactory<>(
                properties,
                new StringSerializer(),
                valueSerializer
        );
    }

    @Bean
    public KafkaTemplate<String, Object>
    paymentKafkaTemplate() {

        return new KafkaTemplate<>(
                paymentKafkaProducerFactory()
        );
    }
}