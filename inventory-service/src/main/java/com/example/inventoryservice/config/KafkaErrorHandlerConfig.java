package com.example.inventoryservice.config;

import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import com.example.commonevents.order.OrderCreatedEvent;
import com.example.inventoryservice.exception.ActiveReservationNotFoundException;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.exception.InvalidOrderCreatedEventException;
import com.example.inventoryservice.exception.InvalidStockQuantityException;
import com.example.inventoryservice.exception.InventoryNotFoundException;
import com.example.inventoryservice.exception.ReservationAlreadyExistsException;
import com.example.inventoryservice.mapper.InventoryReservationFailureEventFactory;
import com.example.inventoryservice.outbox.InventoryFailureOutboxService;
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

    private static final String DLT_SUFFIX =
            "-dlt";

    private final KafkaTemplate<String, Object>
            inventoryKafkaTemplate;

    private final InventoryFailureOutboxService
            inventoryFailureOutboxService;

    private final InventoryReservationFailureEventFactory
            failureEventFactory;

    private final long retryIntervalMs;
    private final long maxRetries;

    public KafkaErrorHandlerConfig(
            @Qualifier("inventoryKafkaTemplate")
            KafkaTemplate<String, Object>
                    inventoryKafkaTemplate,

            InventoryFailureOutboxService
                    inventoryFailureOutboxService,

            InventoryReservationFailureEventFactory
                    failureEventFactory,

            @Value("${inventory.kafka.retry.interval-ms}")
            long retryIntervalMs,

            @Value("${inventory.kafka.retry.max-retries}")
            long maxRetries
    ) {
        this.inventoryKafkaTemplate =
                inventoryKafkaTemplate;

        this.inventoryFailureOutboxService =
                inventoryFailureOutboxService;

        this.failureEventFactory =
                failureEventFactory;

        this.retryIntervalMs =
                retryIntervalMs;

        this.maxRetries =
                maxRetries;
    }

    public DefaultErrorHandler createErrorHandler() {

        DeadLetterPublishingRecoverer dltRecoverer =
                createDeadLetterPublishingRecoverer();

        ConsumerRecordRecoverer recordRecoverer =
                (record, exception) -> {

                    Throwable rootCause =
                            findRootCause(exception);

                    log.error(
                            "Kafka event recovery started. "
                                    + "topic={}, partition={}, offset={}, "
                                    + "exception={}, message={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            rootCause.getClass()
                                    .getSimpleName(),
                            rootCause.getMessage(),
                            exception
                    );

                    /*
                     * Mesaj OrderCreatedEvent olarak deserialize
                     * edilebildiyse orderId bilgisine ulaşabiliriz.
                     *
                     * Reservation işlemi başarısız olduğu için asıl
                     * transaction rollback olmuştur. Failure Outbox
                     * kaydı REQUIRES_NEW transaction ile oluşturulur.
                     */
                    if (record.value()
                            instanceof OrderCreatedEvent
                            orderCreatedEvent) {

                        saveReservationFailureOutboxEvent(
                                orderCreatedEvent,
                                exception
                        );

                    } else {
                        /*
                         * Bozuk JSON gibi deserialization hatalarında
                         * OrderCreatedEvent oluşmadığı için orderId
                         * bilgisine erişemeyiz.
                         *
                         * Bu nedenle failure business event'i
                         * oluşturulamaz; orijinal mesaj yalnızca
                         * DLT'ye gönderilir.
                         */
                        log.warn(
                                "InventoryReservationFailedEvent "
                                        + "could not be created because "
                                        + "the Kafka value was not "
                                        + "deserialized as an "
                                        + "OrderCreatedEvent. "
                                        + "topic={}, partition={}, offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset()
                        );
                    }

                    /*
                     * Failure Outbox kaydı güvenli şekilde
                     * oluşturulduktan sonra orijinal order-created
                     * mesajını DLT'ye göndeririz.
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
         * Kalıcı business veya validation hatalarıdır.
         * Tekrar deneme sonucu değiştirmeyeceği için doğrudan
         * recoverer çalışır.
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
         * Optimistic locking ve geçici altyapı hataları bu
         * listeye eklenmediği için retry edilebilir durumda kalır.
         */
        errorHandler.setRetryListeners(
                (record, exception, deliveryAttempt) -> {

                    Throwable rootCause =
                            findRootCause(exception);

                    log.warn(
                            "Kafka event processing failed. "
                                    + "topic={}, partition={}, offset={}, "
                                    + "deliveryAttempt={}, exception={}, "
                                    + "message={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            deliveryAttempt,
                            rootCause.getClass()
                                    .getSimpleName(),
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
                                        record.topic()
                                                + DLT_SUFFIX,
                                        record.partition()
                                )
                );

        /*
         * DLT publish başarısız olursa mesajın başarılı şekilde
         * recover edilmiş kabul edilmesini engeller.
         */
        recoverer.setFailIfSendResultIsError(
                true
        );

        return recoverer;
    }

    private void saveReservationFailureOutboxEvent(
            OrderCreatedEvent event,
            Exception exception
    ) {
        InventoryReservationFailedEvent failureEvent =
                failureEventFactory.create(
                        event,
                        exception
                );

        inventoryFailureOutboxService
                .saveFailureEvent(
                        failureEvent
                );

        log.info(
                "InventoryReservationFailedEvent saved "
                        + "to Outbox. orderId={}, errorCode={}",
                failureEvent.orderId(),
                failureEvent.errorCode()
        );
    }

    private Throwable findRootCause(
            Throwable throwable
    ) {
        Throwable rootCause =
                throwable;

        while (rootCause.getCause() != null
                && rootCause.getCause()
                != rootCause) {

            rootCause =
                    rootCause.getCause();
        }

        return rootCause;
    }
}