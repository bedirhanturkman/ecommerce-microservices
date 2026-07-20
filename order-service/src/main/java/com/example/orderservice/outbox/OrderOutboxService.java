package com.example.orderservice.outbox;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.orderservice.exception.OrderOutboxSerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderOutboxService {

    private static final String ORDER_AGGREGATE =
            "Order";

    private final OrderOutboxEventRepository
            outboxEventRepository;

    private final ObjectMapper objectMapper;
    private final String orderCreatedTopic;

    public OrderOutboxService(
            OrderOutboxEventRepository
                    outboxEventRepository,

            ObjectMapper objectMapper,

            @Value("${order.kafka.topics.order-created}")
            String orderCreatedTopic
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.objectMapper = objectMapper;
        this.orderCreatedTopic = orderCreatedTopic;
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public void saveOrderCreatedEvent(
            OrderCreatedEvent event
    ) {
        String payload = serialize(event);
        Instant now = Instant.now();

        OrderOutboxEvent outboxEvent =
                OrderOutboxEvent.builder()
                        .id(UUID.randomUUID())
                        .aggregateType(
                                ORDER_AGGREGATE
                        )
                        .aggregateId(
                                String.valueOf(
                                        event.orderId()
                                )
                        )
                        .eventType(
                                event.getClass()
                                        .getName()
                        )
                        .topic(
                                orderCreatedTopic
                        )
                        .messageKey(
                                String.valueOf(
                                        event.orderId()
                                )
                        )
                        .payload(payload)
                        .status(
                                OrderOutboxStatus.PENDING
                        )
                        .retryCount(0)
                        .nextAttemptAt(now)
                        .createdAt(now)
                        .publishedAt(null)
                        .lastError(null)
                        .build();

        outboxEventRepository.save(
                outboxEvent
        );
    }

    private String serialize(
            OrderCreatedEvent event
    ) {
        try {
            return objectMapper
                    .writeValueAsString(event);

        } catch (JsonProcessingException exception) {
            throw new OrderOutboxSerializationException(
                    event.getClass().getName(),
                    exception
            );
        }
    }
}