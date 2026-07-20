package com.example.orderservice.config;

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

    @Value("${order.kafka.producer.max-block-ms}")
    private long maxBlockMs;

    @Value("${order.kafka.producer.request-timeout-ms}")
    private int requestTimeoutMs;

    @Value("${order.kafka.producer.delivery-timeout-ms}")
    private int deliveryTimeoutMs;

    @Bean
    public ProducerFactory<String, Object>
    orderKafkaProducerFactory() {

        Map<String, Object> properties =
                new LinkedHashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        /*
         * send() çağrısının metadata veya producer buffer
         * beklerken en fazla ne kadar bloklanacağını sınırlar.
         */
        properties.put(
                ProducerConfig.MAX_BLOCK_MS_CONFIG,
                maxBlockMs
        );

        /*
         * Bir Kafka request'inin broker cevabını ne kadar
         * bekleyeceğini sınırlar.
         */
        properties.put(
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                requestTimeoutMs
        );

        /*
         * Producer'ın bir kaydı başarıyla teslim etmek veya
         * başarısız olarak tamamlamak için kullanabileceği
         * toplam süreyi sınırlar.
         */
        properties.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                deliveryTimeoutMs
        );

        /*
         * Broker bağlantısı kesildiğinde bağlantı denemeleri
         * arasındaki bekleme süresini kontrollü tutar.
         */
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

        /*
         * Deserialization hatasıyla DLT'ye gönderilen
         * orijinal ham mesajlar için.
         */
        serializers.put(
                byte[].class,
                new ByteArraySerializer()
        );

        /*
         * Normal Java event nesneleri için.
         */
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
    orderKafkaTemplate() {

        return new KafkaTemplate<>(
                orderKafkaProducerFactory()
        );
    }
}