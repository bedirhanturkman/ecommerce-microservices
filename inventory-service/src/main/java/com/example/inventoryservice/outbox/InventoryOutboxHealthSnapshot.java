package com.example.inventoryservice.outbox;

import java.time.Instant;

public record InventoryOutboxHealthSnapshot(
        long pendingCount,
        Instant oldestPendingCreatedAt,
        long oldestPendingAgeSeconds,
        long highRetryPendingCount
) {
}