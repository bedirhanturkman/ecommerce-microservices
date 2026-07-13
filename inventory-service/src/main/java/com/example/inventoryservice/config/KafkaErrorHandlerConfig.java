package com.example.inventoryservice.config;

import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.exception.InvalidOrderCreatedEventException;
import com.example.inventoryservice.exception.InvalidStockQuantityException;
import com.example.inventoryservice.exception.InventoryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Component
public class KafkaErrorHandlerConfig {

    private static final String DLT_SUFFIX = "-dlt";

    private final KafkaTemplate<String, Object> dltKafkaTemplate;
    private final long retryIntervalMs;
    private final long maxRetries;

    public KafkaErrorHandlerConfig(
            KafkaTemplate<String, Object> dltKafkaTemplate,
            @Value("${inventory.kafka.retry.interval-ms}") long retryIntervalMs,
            @Value("${inventory.kafka.retry.max-retries}") long maxRetries
    ) {
        this.dltKafkaTemplate = dltKafkaTemplate;
        this.retryIntervalMs = retryIntervalMs;
        this.maxRetries = maxRetries;
    }

    public DefaultErrorHandler createErrorHandler() {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        dltKafkaTemplate,
                        (record, exception) -> new TopicPartition(
                                record.topic() + DLT_SUFFIX,
                                record.partition()
                        )
                );

        FixedBackOff fixedBackOff =
                new FixedBackOff(
                        retryIntervalMs,
                        maxRetries
                );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        fixedBackOff
                );

        errorHandler.addNotRetryableExceptions(
                InvalidOrderCreatedEventException.class,
                InventoryNotFoundException.class,
                InsufficientStockException.class,
                InvalidStockQuantityException.class
        );

        errorHandler.setRetryListeners(
                (record, exception, deliveryAttempt) ->
                        log.warn(
                                "Kafka event processing failed. " +
                                        "topic={}, partition={}, offset={}, attempt={}, exception={}, message={}",
                                record.topic(),
                                record.partition(),
                                record.offset(),
                                deliveryAttempt,
                                exception.getClass().getSimpleName(),
                                exception.getMessage()
                        )
        );

        return errorHandler;
    }
}