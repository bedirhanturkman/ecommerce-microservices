package com.example.commonevents.inventory;

import java.time.Instant;
import java.util.List;

public record InventoryReservationConfirmedEvent(
        Long orderId,
        List<InventoryReservationItem> reservations,
        Instant confirmedAt
) {
}
