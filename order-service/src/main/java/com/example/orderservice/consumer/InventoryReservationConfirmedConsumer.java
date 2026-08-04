package com.example.orderservice.consumer;

import com.example.commonevents.inventory.InventoryReservationConfirmedEvent;
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
public class InventoryReservationConfirmedConsumer {
    private final OrderSagaProjectionService projectionService;
    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${order.kafka.topics.inventory-reservation-confirmed}",
            containerFactory = "inventoryConfirmedKafkaListenerContainerFactory")
    public void consume(
            InventoryReservationConfirmedEvent event,
            @Header("outbox-event-id") byte[] eventId
    ) {
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));
        projectionService.applyConfirmed(toUuid(eventId), event, order.getStatus());
    }

    private UUID toUuid(byte[] value) {
        return UUID.fromString(new String(value, StandardCharsets.UTF_8));
    }
}
