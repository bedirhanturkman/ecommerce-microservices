package com.example.commonevents.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InventoryReservedEvent(
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        List<ReservedInventoryItem> reservedItems,
        Instant reservedAt
) {
}