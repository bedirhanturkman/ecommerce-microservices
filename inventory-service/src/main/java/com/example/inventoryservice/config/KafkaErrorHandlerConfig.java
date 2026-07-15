package com.example.inventoryservice.config;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.inventoryservice.exception.ActiveReservationNotFoundException;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.exception.InvalidOrderCreatedEventException;
import com.example.inventoryservice.exception.InvalidStockQuantityException;
import com.example.inventoryservice.exception.InventoryNotFoundException;
import com.example.inventoryservice.exception.ReservationAlreadyExistsException;
import com.example.inventoryservice.mapper.InventoryReservationFailureEventFactory;
import com.example.inventoryservice.producer.InventoryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaErrorHandlerConfig {

    private static final String DLT_SUFFIX = "-dlt";

    private final KafkaTemplate<String, Object> inventoryKafkaTemplate;
    private final InventoryEventProducer inventoryEventProducer;
    private final InventoryReservationFailureEventFactory failureEventFactory;

    private final long retryIntervalMs;
    private final long maxRetries;

    public KafkaErrorHandlerConfig(
            @Qualifier("inventoryKafkaTemplate")
            KafkaTemplate<String, Object> inventoryKafkaTemplate,

            InventoryEventProducer inventoryEventProducer,

            InventoryReservationFailureEventFactory failureEventFactory,

            @Value("${inventory.kafka.retry.interval-ms}")
            long retryIntervalMs,

            @Value("${inventory.kafka.retry.max-retries}")
            long maxRetries
    ) {
        this.inventoryKafkaTemplate = inventoryKafkaTemplate;
        this.inventoryEventProducer = inventoryEventProducer;
        this.failureEventFactory = failureEventFactory;
        this.retryIntervalMs = retryIntervalMs;
        this.maxRetries = maxRetries;
    }

    public DefaultErrorHandler createErrorHandler() {

        DeadLetterPublishingRecoverer dltRecoverer =
                createDeadLetterPublishingRecoverer();

        ConsumerRecordRecoverer recordRecoverer =
                (record, exception) -> {

                    Throwable rootCause =
                            findRootCause(exception);

                    log.error(
                            "Kafka event recovery started. " +
                                    "topic={}, partition={}, offset={}, " +
                                    "exception={}, message={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            rootCause.getClass().getSimpleName(),
                            rootCause.getMessage(),
                            exception
                    );

                    /*
                     * Event deserialize edilebildiyse orderId bilgisine
                     * ulaşabilir ve InventoryReservationFailedEvent
                     * yayınlayabiliriz.
                     *
                     * Bozuk JSON durumunda record.value() bir
                     * OrderCreatedEvent olmayacağı için yalnızca DLT'ye
                     * gönderilir.
                     */
                    if (record.value()
                            instanceof OrderCreatedEvent orderCreatedEvent) {

                        publishReservationFailureEvent(
                                orderCreatedEvent,
                                exception
                        );

                    } else {
                        log.warn(
                                "InventoryReservationFailedEvent could not " +
                                        "be created because the Kafka value " +
                                        "was not deserialized as an " +
                                        "OrderCreatedEvent. " +
                                        "topic={}, partition={}, offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset()
                        );
                    }

                    /*
                     * Failure event işleminden sonra orijinal Kafka
                     * kaydını DLT'ye gönder.
                     */
                    dltRecoverer.accept(
                            record,
                            exception
                    );
                };

        FixedBackOff fixedBackOff =
                new FixedBackOff(
                        retryIntervalMs,
                        maxRetries
                );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recordRecoverer,
                        fixedBackOff
                );

        /*
         * Bunlar kalıcı business veya validation hatalarıdır.
         * Tekrar denemek sonucu değiştirmeyeceği için doğrudan
         * recoverer çalışır:
         *
         * 1. InventoryReservationFailedEvent yayınlanır.
         * 2. Orijinal mesaj order-created-dlt'ye gönderilir.
         */
        errorHandler.addNotRetryableExceptions(
                InvalidOrderCreatedEventException.class,
                InventoryNotFoundException.class,
                InsufficientStockException.class,
                InvalidStockQuantityException.class,
                ReservationAlreadyExistsException.class,
                ActiveReservationNotFoundException.class,
                DeserializationException.class
        );

        /*
         * OptimisticLockingFailureException bu listeye özellikle
         * eklenmedi. Eş zamanlı transaction kaynaklı olabileceği
         * için retry uygulanmaya devam edecek.
         */
        errorHandler.setRetryListeners(
                (record, exception, deliveryAttempt) -> {

                    Throwable rootCause =
                            findRootCause(exception);

                    log.warn(
                            "Kafka event processing failed. " +
                                    "topic={}, partition={}, offset={}, " +
                                    "deliveryAttempt={}, exception={}, " +
                                    "message={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            deliveryAttempt,
                            rootCause.getClass().getSimpleName(),
                            rootCause.getMessage()
                    );
                }
        );

        return errorHandler;
    }

    private DeadLetterPublishingRecoverer
    createDeadLetterPublishingRecoverer() {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        inventoryKafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + DLT_SUFFIX,
                                        record.partition()
                                )
                );

        /*
         * DLT publish işlemi başarısız olursa recoverer'ın başarılı
         * olmuş gibi davranmasını engeller.
         */
        recoverer.setFailIfSendResultIsError(true);

        return recoverer;
    }

    private void publishReservationFailureEvent(
            OrderCreatedEvent event,
            Exception exception
    ) {
        try {
            inventoryEventProducer.publishReservationFailed(
                    failureEventFactory.create(
                            event,
                            exception
                    )
            );

        } catch (RuntimeException publishException) {

            log.error(
                    "InventoryReservationFailedEvent publish operation " +
                            "could not be started. orderId={}",
                    event.orderId(),
                    publishException
            );
        }
    }

    private Throwable findRootCause(
            Throwable throwable
    ) {
        Throwable rootCause = throwable;

        while (rootCause.getCause() != null
                && rootCause.getCause() != rootCause) {

            rootCause = rootCause.getCause();
        }

        return rootCause;
    }
}