package com.example.inventoryservice.service.model;

import com.example.commonevents.inventory.ReservedInventoryItem;

import java.time.Instant;
import java.util.List;

public record ReservationResult(
        Long orderId,
        List<ReservedInventoryItem> reservedItems,
        Instant reservedAt
) {
}