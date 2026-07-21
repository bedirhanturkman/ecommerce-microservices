package com.example.paymentservice.outbox;

import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
import com.example.paymentservice.exception.PaymentOutboxSerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentOutboxService {

    private static final String PAYMENT_AGGREGATE =
            "Payment";

    private final PaymentOutboxEventRepository
            outboxEventRepository;

    private final ObjectMapper objectMapper;

    private final String paymentSucceededTopic;
    private final String paymentFailedTopic;

    public PaymentOutboxService(
            PaymentOutboxEventRepository
                    outboxEventRepository,

            ObjectMapper objectMapper,

            @Value("${payment.kafka.topics.succeeded}")
            String paymentSucceededTopic,

            @Value("${payment.kafka.topics.failed}")
            String paymentFailedTopic
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.objectMapper =
                objectMapper;

        this.paymentSucceededTopic =
                paymentSucceededTopic;

        this.paymentFailedTopic =
                paymentFailedTopic;
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public boolean savePaymentSucceededEvent(
            PaymentSucceededEvent event
    ) {
        return saveEvent(
                event.orderId(),
                event,
                paymentSucceededTopic
        );
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public boolean savePaymentFailedEvent(
            PaymentFailedEvent event
    ) {
        return saveEvent(
                event.orderId(),
                event,
                paymentFailedTopic
        );
    }

    private boolean saveEvent(
            Long orderId,
            Object event,
            String topic
    ) {
        String eventType =
                event.getClass().getName();

        String payload =
                serialize(
                        event,
                        eventType
                );

        Instant now =
                Instant.now();

        int insertedRowCount =
                outboxEventRepository
                        .insertPendingIfAbsent(
                                UUID.randomUUID(),
                                PAYMENT_AGGREGATE,
                                String.valueOf(orderId),
                                eventType,
                                topic,
                                String.valueOf(orderId),
                                payload,
                                now
                        );

        return insertedRowCount == 1;
    }

    private String serialize(
            Object event,
            String eventType
    ) {
        try {
            return objectMapper
                    .writeValueAsString(event);

        } catch (JsonProcessingException exception) {
            throw new PaymentOutboxSerializationException(
                    eventType,
                    exception
            );
        }
    }
}