package com.example.inventoryservice.service;

import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.commonevents.order.OrderCreatedEvent;
import com.example.inventoryservice.outbox.InventoryOutboxService;
import com.example.inventoryservice.service.model.ReservationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreatedEventProcessingService {

    private final ReservationService reservationService;

    private final ProcessedEventService processedEventService;

    private final InventoryOutboxService inventoryOutboxService;

    @Transactional
    public boolean process(
            OrderCreatedEvent event
    ) {
        /*
         * Aynı OrderCreatedEvent daha önce başarıyla
         * işlendiyse stok rezervasyonu tekrar yapılmaz.
         */
        if (processedEventService.isProcessed(
                event.orderId()
        )) {
            return false;
        }

        ReservationResult result =
                reservationService.reserveStock(
                        event.orderId(),
                        event.items()
                );

        InventoryReservedEvent inventoryReservedEvent =
                new InventoryReservedEvent(
                        event.orderId(),
                        event.customerId(),
                        event.totalPrice(),
                        result.reservedItems(),
                        result.reservedAt()
                );

        /*
         * InventoryReservedEvent doğrudan Kafka'ya
         * gönderilmez. Aynı database transaction
         * içerisinde Outbox'a PENDING olarak yazılır.
         */
        inventoryOutboxService
                .saveInventoryReservedEvent(
                        inventoryReservedEvent
                );

        /*
         * Processed event kaydı da aynı transaction
         * içerisindedir.
         */
        processedEventService.markAsProcessed(
                event.orderId()
        );

        return true;
    }
}