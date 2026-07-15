package com.example.inventoryservice.config;

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

    @Bean
    public ProducerFactory<String, Object>
    inventoryKafkaProducerFactory() {

        Map<String, Object> properties =
                new LinkedHashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
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
    inventoryKafkaTemplate() {

        return new KafkaTemplate<>(
                inventoryKafkaProducerFactory()
        );
    }
}