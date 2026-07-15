package com.example.inventoryservice.event.listener;

import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.inventoryservice.event.internal.InventoryReservationCompletedEvent;
import com.example.inventoryservice.producer.InventoryEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InventoryReservationEventListener {

    private final InventoryEventProducer inventoryEventProducer;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleReservationCompleted(
            InventoryReservationCompletedEvent event
    ) {
        InventoryReservedEvent kafkaEvent =
                new InventoryReservedEvent(
                        event.orderId(),
                        event.reservedItems(),
                        event.reservedAt()
                );

        inventoryEventProducer.publishInventoryReserved(
                kafkaEvent
        );
    }
}