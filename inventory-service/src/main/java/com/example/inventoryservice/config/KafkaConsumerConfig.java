package com.example.inventoryservice.config;

import com.example.commonevents.order.OrderCreatedEvent;
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

    private static final String ORDER_EVENT_PACKAGE =
            "com.example.commonevents.order";

    private static final String PAYMENT_EVENT_PACKAGE =
            "com.example.commonevents.payment";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    /*
     * =========================================================
     * OrderCreatedEvent Consumer Factory
     * =========================================================
     */

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent>
    orderCreatedConsumerFactory() {

        return createConsumerFactory(
                OrderCreatedEvent.class,
                ORDER_EVENT_PACKAGE
        );
    }

    /*
     * =========================================================
     * PaymentSucceededEvent Consumer Factory
     * =========================================================
     */

    @Bean
    public ConsumerFactory<String, PaymentSucceededEvent>
    paymentSucceededConsumerFactory() {

        return createConsumerFactory(
                PaymentSucceededEvent.class,
                PAYMENT_EVENT_PACKAGE
        );
    }

    /*
     * =========================================================
     * PaymentFailedEvent Consumer Factory
     * =========================================================
     */

    @Bean
    public ConsumerFactory<String, PaymentFailedEvent>
    paymentFailedConsumerFactory() {

        return createConsumerFactory(
                PaymentFailedEvent.class,
                PAYMENT_EVENT_PACKAGE
        );
    }

    /*
     * =========================================================
     * OrderCreatedEvent Listener Factory
     *
     * Reservation oluşturma sırasında oluşan hatalar için
     * mevcut KafkaErrorHandlerConfig kullanılır.
     * =========================================================
     */

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

    /*
     * =========================================================
     * PaymentSucceededEvent Listener Factory
     *
     * Başarılı ödeme sonucunda reservation CONFIRMED yapılır.
     * Payment sonucu için ayrı error handler kullanılır.
     * =========================================================
     */

    @Bean
    public ConcurrentKafkaListenerContainerFactory
            <String, PaymentSucceededEvent>
    paymentSucceededKafkaListenerContainerFactory(
            PaymentResultKafkaErrorHandlerConfig
                    paymentResultKafkaErrorHandlerConfig
    ) {
        ConcurrentKafkaListenerContainerFactory
                <String, PaymentSucceededEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                paymentSucceededConsumerFactory()
        );

        factory.setCommonErrorHandler(
                paymentResultKafkaErrorHandlerConfig
                        .createErrorHandler()
        );

        return factory;
    }

    /*
     * =========================================================
     * PaymentFailedEvent Listener Factory
     *
     * Başarısız ödeme sonucunda reservation RELEASED yapılır.
     * Payment sonucu için ayrı error handler kullanılır.
     * =========================================================
     */

    @Bean
    public ConcurrentKafkaListenerContainerFactory
            <String, PaymentFailedEvent>
    paymentFailedKafkaListenerContainerFactory(
            PaymentResultKafkaErrorHandlerConfig
                    paymentResultKafkaErrorHandlerConfig
    ) {
        ConcurrentKafkaListenerContainerFactory
                <String, PaymentFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                paymentFailedConsumerFactory()
        );

        factory.setCommonErrorHandler(
                paymentResultKafkaErrorHandlerConfig
                        .createErrorHandler()
        );

        return factory;
    }

    /*
     * =========================================================
     * Ortak Generic Consumer Factory
     * =========================================================
     */

    private <T> ConsumerFactory<String, T>
    createConsumerFactory(
            Class<T> eventClass,
            String trustedPackage
    ) {
        Map<String, Object> properties =
                createConsumerProperties();

        JacksonJsonDeserializer<T> jsonDeserializer =
                new JacksonJsonDeserializer<>(
                        eventClass
                );

        /*
         * Her factory belirli bir event tipine bağlı.
         * Kafka type header'ının hedef sınıfı değiştirmesine
         * izin vermiyoruz.
         */
        jsonDeserializer.setUseTypeHeaders(false);

        jsonDeserializer.addTrustedPackages(
                trustedPackage
        );

        ErrorHandlingDeserializer<T>
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

    /*
     * =========================================================
     * Ortak Kafka Consumer Ayarları
     * =========================================================
     */

    private Map<String, Object>
    createConsumerProperties() {

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

        /*
         * Offset commit işlemini Kafka'nın zaman bazlı otomatik
         * commit mekanizmasına bırakmıyoruz. Listener container
         * mesaj işleme sonucuna göre offset yönetir.
         */
        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        return properties;
    }
}