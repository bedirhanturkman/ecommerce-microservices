package com.example.inventoryservice.service;

import com.example.commonevents.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreatedEventProcessingService {

    private final ReservationService reservationService;
    private final ProcessedEventService processedEventService;

    @Transactional
    public boolean process(OrderCreatedEvent event) {

        if (processedEventService.isProcessed(event.orderId())) {
            return false;
        }

        reservationService.reserveStock(
                event.orderId(),
                event.items()
        );

        processedEventService.markAsProcessed(
                event.orderId()
        );

        return true;
    }
}