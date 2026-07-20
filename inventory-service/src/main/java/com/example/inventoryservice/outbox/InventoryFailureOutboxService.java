package com.example.inventoryservice.outbox;

import com.example.commonevents.inventory.InventoryReservationFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryFailureOutboxService {

    private final InventoryOutboxService
            inventoryOutboxService;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void saveFailureEvent(
            InventoryReservationFailedEvent event
    ) {
        boolean created =
                inventoryOutboxService
                        .saveInventoryReservationFailedEvent(
                                event
                        );

        if (!created) {
            log.info(
                    "Inventory reservation failure outbox event "
                            + "already exists. orderId={}",
                    event.orderId()
            );

            return;
        }

        log.info(
                "Inventory reservation failure outbox event created. "
                        + "orderId={}, errorCode={}",
                event.orderId(),
                event.errorCode()
        );
    }
}