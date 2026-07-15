package com.example.inventoryservice.event.internal;

import com.example.commonevents.inventory.ReservedInventoryItem;

import java.time.Instant;
import java.util.List;

public record InventoryReservationCompletedEvent(
        Long orderId,
        List<ReservedInventoryItem> reservedItems,
        Instant reservedAt
) {
}