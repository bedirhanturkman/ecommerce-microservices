package com.example.inventoryservice.config;

import com.example.inventoryservice.exception.ActiveReservationNotFoundException;
import com.example.inventoryservice.exception.InvalidPaymentResultEventException;
import com.example.inventoryservice.exception.InventoryNotFoundException;
import com.example.inventoryservice.exception.ReservationStateConflictException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class PaymentResultKafkaErrorHandlerConfig {

    private static final String DLT_SUFFIX = "-dlt";

    private final KafkaTemplate<String, Object>
            inventoryKafkaTemplate;

    private final long retryIntervalMs;
    private final long maxRetries;

    public PaymentResultKafkaErrorHandlerConfig(
            @Qualifier("inventoryKafkaTemplate")
            KafkaTemplate<String, Object>
                    inventoryKafkaTemplate,

            @Value("${inventory.kafka.retry.interval-ms}")
            long retryIntervalMs,

            @Value("${inventory.kafka.retry.max-retries}")
            long maxRetries
    ) {
        this.inventoryKafkaTemplate =
                inventoryKafkaTemplate;

        this.retryIntervalMs = retryIntervalMs;
        this.maxRetries = maxRetries;
    }

    public DefaultErrorHandler createErrorHandler() {

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

        recoverer.setFailIfSendResultIsError(
                true
        );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        new FixedBackOff(
                                retryIntervalMs,
                                maxRetries
                        )
                );

        errorHandler.addNotRetryableExceptions(
                InvalidPaymentResultEventException.class,
                ActiveReservationNotFoundException.class,
                InventoryNotFoundException.class,
                ReservationStateConflictException.class,
                DeserializationException.class,
                IllegalStateException.class
        );

        errorHandler.setRetryListeners(
                (record, exception, deliveryAttempt) -> {

                    Throwable rootCause =
                            findRootCause(exception);

                    log.warn(
                            "Payment result event processing failed. " +
                                    "topic={}, partition={}, offset={}, " +
                                    "deliveryAttempt={}, exception={}, " +
                                    "message={}",
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

    private Throwable findRootCause(
            Throwable throwable
    ) {
        Throwable rootCause = throwable;

        while (rootCause.getCause() != null
                && rootCause.getCause()
                != rootCause) {

            rootCause = rootCause.getCause();
        }

        return rootCause;
    }
}