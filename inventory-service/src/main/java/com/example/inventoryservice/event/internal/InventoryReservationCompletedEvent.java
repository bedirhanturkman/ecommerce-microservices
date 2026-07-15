package com.example.inventoryservice.event.internal;

import com.example.commonevents.inventory.ReservedInventoryItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InventoryReservationCompletedEvent(
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        List<ReservedInventoryItem> reservedItems,
        Instant reservedAt
) {
}