package com.example.inventoryservice.service;

import com.example.commonevents.order.OrderCreatedEvent;
import com.example.inventoryservice.event.internal.InventoryReservationCompletedEvent;
import com.example.inventoryservice.service.model.ReservationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreatedEventProcessingService {

    private final ReservationService reservationService;
    private final ProcessedEventService processedEventService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public boolean process(OrderCreatedEvent event) {

        if (processedEventService.isProcessed(event.orderId())) {
            return false;
        }

        ReservationResult result =
                reservationService.reserveStock(
                        event.orderId(),
                        event.items()
                );

        processedEventService.markAsProcessed(
                event.orderId()
        );

        applicationEventPublisher.publishEvent(
                new InventoryReservationCompletedEvent(
                        result.orderId(),
                        result.reservedItems(),
                        result.reservedAt()
                )
        );

        return true;
    }
}