package com.example.orderservice.config;

import com.example.orderservice.exception.InvalidOrderResultEventException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.OrderResultMismatchException;
import com.example.orderservice.exception.OrderStateConflictException;
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
public class OrderResultKafkaErrorHandlerConfig {

    private static final String ORDER_DLT_SUFFIX =
            "-order-dlt";

    private final KafkaTemplate<String, Object>
            orderKafkaTemplate;

    private final long retryIntervalMs;
    private final long maxRetries;

    public OrderResultKafkaErrorHandlerConfig(
            @Qualifier("orderKafkaTemplate")
            KafkaTemplate<String, Object>
                    orderKafkaTemplate,

            @Value("${order.kafka.retry.interval-ms}")
            long retryIntervalMs,

            @Value("${order.kafka.retry.max-retries}")
            long maxRetries
    ) {
        this.orderKafkaTemplate =
                orderKafkaTemplate;

        this.retryIntervalMs = retryIntervalMs;
        this.maxRetries = maxRetries;
    }

    public DefaultErrorHandler createErrorHandler() {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        orderKafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic()
                                                + ORDER_DLT_SUFFIX,
                                        record.partition()
                                )
                );

        recoverer.setFailIfSendResultIsError(true);

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        new FixedBackOff(
                                retryIntervalMs,
                                maxRetries
                        )
                );

        errorHandler.addNotRetryableExceptions(
                InvalidOrderResultEventException.class,
                OrderNotFoundException.class,
                OrderResultMismatchException.class,
                OrderStateConflictException.class,
                DeserializationException.class
        );

        errorHandler.setRetryListeners(
                (record, exception, deliveryAttempt) -> {

                    Throwable rootCause =
                            findRootCause(exception);

                    log.warn(
                            "Order result event processing failed. "
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