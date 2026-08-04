package com.example.inventoryservice.service.model;

import com.example.commonevents.inventory.InventoryReservationItem;

import java.time.Instant;
import java.util.List;

public record ReservationStatusChangeResult(
        boolean changed,
        Long orderId,
        List<InventoryReservationItem> reservations,
        Instant occurredAt
) {
}
