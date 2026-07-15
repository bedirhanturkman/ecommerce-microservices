package com.example.commonevents.inventory;

import java.time.Instant;
import java.util.List;

public record InventoryReservedEvent(
        Long orderId,
        List<ReservedInventoryItem> reservedItems,
        Instant reservedAt
) {
}