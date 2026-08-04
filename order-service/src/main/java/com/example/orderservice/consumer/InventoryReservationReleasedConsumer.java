package com.example.orderservice.consumer;

import com.example.commonevents.inventory.InventoryReservationReleasedEvent;
import com.example.orderservice.entity.Order;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.saga.OrderSagaProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryReservationReleasedConsumer {
    private final OrderSagaProjectionService projectionService;
    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${order.kafka.topics.inventory-reservation-released}",
            containerFactory = "inventoryReleasedKafkaListenerContainerFactory")
    public void consume(
            InventoryReservationReleasedEvent event,
            @Header("outbox-event-id") byte[] eventId
    ) {
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));
        projectionService.applyReleased(toUuid(eventId), event, order.getStatus());
    }

    private UUID toUuid(byte[] value) {
        return UUID.fromString(new String(value, StandardCharsets.UTF_8));
    }
}
