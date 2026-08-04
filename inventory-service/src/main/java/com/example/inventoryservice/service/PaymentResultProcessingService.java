package com.example.inventoryservice.service;

import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
import com.example.commonevents.inventory.InventoryReservationConfirmedEvent;
import com.example.commonevents.inventory.InventoryReservationReleasedEvent;
import com.example.inventoryservice.outbox.InventoryOutboxService;
import com.example.inventoryservice.service.model.ReservationStatusChangeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentResultProcessingService {

    private final ReservationService reservationService;
    private final InventoryOutboxService inventoryOutboxService;

    @Transactional
    public boolean processSucceededPayment(
            PaymentSucceededEvent event
    ) {
        ReservationStatusChangeResult result = reservationService
                .confirmReservation(event.orderId());

        if (result.changed()) {
            inventoryOutboxService.saveInventoryReservationConfirmedEvent(
                    new InventoryReservationConfirmedEvent(
                            result.orderId(),
                            result.reservations(),
                            result.occurredAt()
                    )
            );
        }

        return result.changed();
    }

    @Transactional
    public boolean processFailedPayment(
            PaymentFailedEvent event
    ) {
        ReservationStatusChangeResult result = reservationService
                .releaseReservation(event.orderId());

        if (result.changed()) {
            inventoryOutboxService.saveInventoryReservationReleasedEvent(
                    new InventoryReservationReleasedEvent(
                            result.orderId(),
                            result.reservations(),
                            result.occurredAt()
                    )
            );
        }

        return result.changed();
    }
}
