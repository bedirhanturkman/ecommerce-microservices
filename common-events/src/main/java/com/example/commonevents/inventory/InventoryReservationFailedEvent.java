package com.example.commonevents.inventory;

import java.time.Instant;

public record InventoryReservationFailedEvent(
        Long orderId,
        InventoryReservationErrorCode errorCode,
        String message,
        Instant failedAt
) {
}