package com.example.paymentservice.outbox;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class PaymentOutboxPublishingService {

    private static final int LAST_ERROR_MAX_LENGTH =
            1000;

    private final PaymentOutboxEventRepository
            outboxEventRepository;

    private final KafkaTemplate<String, Object>
            paymentKafkaTemplate;

    private final int batchSize;
    private final long sendTimeoutSeconds;
    private final long retryDelayMs;

    public PaymentOutboxPublishingService(
            PaymentOutboxEventRepository
                    outboxEventRepository,

            @Qualifier("paymentKafkaTemplate")
            KafkaTemplate<String, Object>
                    paymentKafkaTemplate,

            @Value("${payment.outbox.publisher.batch-size}")
            int batchSize,

            @Value("${payment.outbox.publisher.send-timeout-seconds}")
            long sendTimeoutSeconds,

            @Value("${payment.outbox.publisher.retry-delay-ms}")
            long retryDelayMs
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.paymentKafkaTemplate =
                paymentKafkaTemplate;

        this.batchSize =
                batchSize;

        this.sendTimeoutSeconds =
                sendTimeoutSeconds;

        this.retryDelayMs =
                retryDelayMs;
    }

    @Transactional
    public int publishPendingBatch() {

        List<PaymentOutboxEvent> events =
                outboxEventRepository
                        .findPendingBatchForUpdate(
                                batchSize
                        );

        int publishedCount =
                0;

        for (PaymentOutboxEvent event : events) {
            if (publishEvent(event)) {
                publishedCount++;
            }
        }

        return publishedCount;
    }

    private boolean publishEvent(
            PaymentOutboxEvent event
    ) {
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(
                        event.getTopic(),
                        event.getMessageKey(),
                        event.getPayload()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        record.headers().add(
                "outbox-event-id",
                event.getId()
                        .toString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
        );

        record.headers().add(
                "event-type",
                event.getEventType()
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
        );

        try {
            SendResult<String, Object> result =
                    paymentKafkaTemplate
                            .send(record)
                            .get(
                                    sendTimeoutSeconds,
                                    TimeUnit.SECONDS
                            );

            markPublished(event);

            log.info(
                    "Payment outbox event published. "
                            + "outboxEventId={}, aggregateId={}, "
                            + "eventType={}, topic={}, "
                            + "partition={}, offset={}",
                    event.getId(),
                    event.getAggregateId(),
                    event.getEventType(),
                    event.getTopic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );

            return true;

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            markFailed(
                    event,
                    exception
            );

            return false;

        } catch (
                ExecutionException
                | TimeoutException
                | org.apache.kafka.common.KafkaException
                | org.springframework.kafka.KafkaException exception
        ) {
            markFailed(
                    event,
                    exception
            );

            return false;
        }
    }

    private void markPublished(
            PaymentOutboxEvent event
    ) {
        event.setStatus(
                PaymentOutboxStatus.PUBLISHED
        );

        event.setPublishedAt(
                Instant.now()
        );

        event.setLastError(null);
    }

    private void markFailed(
            PaymentOutboxEvent event,
            Exception exception
    ) {
        int nextRetryCount =
                event.getRetryCount() + 1;

        event.setRetryCount(
                nextRetryCount
        );

        event.setNextAttemptAt(
                Instant.now()
                        .plusMillis(
                                retryDelayMs
                        )
        );

        Throwable rootCause =
                findRootCause(exception);

        event.setLastError(
                limitErrorMessage(
                        rootCause.getMessage()
                )
        );

        log.warn(
                "Payment outbox event could not be published. "
                        + "outboxEventId={}, aggregateId={}, "
                        + "eventType={}, retryCount={}, "
                        + "nextAttemptAt={}, error={}",
                event.getId(),
                event.getAggregateId(),
                event.getEventType(),
                nextRetryCount,
                event.getNextAttemptAt(),
                event.getLastError()
        );
    }

    private String limitErrorMessage(
            String message
    ) {
        if (message == null
                || message.isBlank()) {

            return "Unknown Kafka publishing error";
        }

        if (message.length()
                <= LAST_ERROR_MAX_LENGTH) {

            return message;
        }

        return message.substring(
                0,
                LAST_ERROR_MAX_LENGTH
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