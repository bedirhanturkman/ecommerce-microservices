package com.example.orderservice.outbox;

import java.time.Instant;

public record OrderOutboxHealthSnapshot(
        long pendingCount,
        Instant oldestPendingCreatedAt,
        long oldestPendingAgeSeconds,
        long highRetryPendingCount
) {
}