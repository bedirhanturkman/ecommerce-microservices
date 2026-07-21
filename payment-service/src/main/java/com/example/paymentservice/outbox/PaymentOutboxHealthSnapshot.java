package com.example.paymentservice.outbox;

import java.time.Instant;

public record PaymentOutboxHealthSnapshot(
        long pendingCount,
        Instant oldestPendingCreatedAt,
        long oldestPendingAgeSeconds,
        long highRetryPendingCount
) {
}