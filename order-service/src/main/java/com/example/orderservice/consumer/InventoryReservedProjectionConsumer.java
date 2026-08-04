package com.example.orderservice.consumer;

import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.orderservice.saga.OrderSagaProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryReservedProjectionConsumer {
    private final OrderSagaProjectionService projectionService;

    @KafkaListener(
            topics = "${order.kafka.topics.inventory-reserved}",
            containerFactory = "inventoryReservedKafkaListenerContainerFactory")
    public void consume(
            InventoryReservedEvent event,
            @Header("outbox-event-id") byte[] eventId
    ) {
        projectionService.applyReserved(toUuid(eventId), event);
    }

    private UUID toUuid(byte[] value) {
        return UUID.fromString(new String(value, StandardCharsets.UTF_8));
    }
}
