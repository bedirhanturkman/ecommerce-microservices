package com.example.paymentservice.config;

import com.example.paymentservice.exception.InvalidInventoryReservedEventException;
import com.example.paymentservice.exception.PaymentAlreadyExistsException;
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
public class PaymentKafkaErrorHandlerConfig {

    private static final String DLT_SUFFIX = "-dlt";

    private final KafkaTemplate<String, Object>
            paymentKafkaTemplate;

    private final long retryIntervalMs;
    private final long maxRetries;

    public PaymentKafkaErrorHandlerConfig(
            @Qualifier("paymentKafkaTemplate")
            KafkaTemplate<String, Object>
                    paymentKafkaTemplate,

            @Value("${payment.kafka.retry.interval-ms}")
            long retryIntervalMs,

            @Value("${payment.kafka.retry.max-retries}")
            long maxRetries
    ) {
        this.paymentKafkaTemplate =
                paymentKafkaTemplate;

        this.retryIntervalMs = retryIntervalMs;
        this.maxRetries = maxRetries;
    }

    public DefaultErrorHandler createErrorHandler() {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        paymentKafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic()
                                                + DLT_SUFFIX,
                                        record.partition()
                                )
                );

        recoverer.setFailIfSendResultIsError(true);

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
                InvalidInventoryReservedEventException.class,
                PaymentAlreadyExistsException.class,
                DeserializationException.class
        );

        errorHandler.setRetryListeners(
                (record, exception, deliveryAttempt) -> {

                    Throwable rootCause =
                            findRootCause(exception);

                    log.warn(
                            "Payment Kafka event processing failed. " +
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