package com.example.inventoryservice.outbox;

import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.inventoryservice.exception.InventoryOutboxSerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InventoryOutboxService {

    private static final String INVENTORY_AGGREGATE =
            "InventoryReservation";

    private final InventoryOutboxEventRepository
            outboxEventRepository;

    private final ObjectMapper objectMapper;

    private final String inventoryReservedTopic;
    private final String inventoryReservationFailedTopic;

    public InventoryOutboxService(
            InventoryOutboxEventRepository
                    outboxEventRepository,

            ObjectMapper objectMapper,

            @Value("${inventory.kafka.topics.reserved}")
            String inventoryReservedTopic,

            @Value("${inventory.kafka.topics.reservation-failed}")
            String inventoryReservationFailedTopic
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.objectMapper = objectMapper;

        this.inventoryReservedTopic =
                inventoryReservedTopic;

        this.inventoryReservationFailedTopic =
                inventoryReservationFailedTopic;
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public boolean saveInventoryReservedEvent(
            InventoryReservedEvent event
    ) {
        return saveEvent(
                event.orderId(),
                event,
                inventoryReservedTopic
        );
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public boolean saveInventoryReservationFailedEvent(
            InventoryReservationFailedEvent event
    ) {
        return saveEvent(
                event.orderId(),
                event,
                inventoryReservationFailedTopic
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

        Instant now = Instant.now();

        int insertedRowCount =
                outboxEventRepository
                        .insertPendingIfAbsent(
                                UUID.randomUUID(),
                                INVENTORY_AGGREGATE,
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
            throw new InventoryOutboxSerializationException(
                    eventType,
                    exception
            );
        }
    }
}