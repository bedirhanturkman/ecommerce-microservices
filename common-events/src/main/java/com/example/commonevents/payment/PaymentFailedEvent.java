package com.example.commonevents.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        PaymentFailureCode failureCode,
        String failureReason,
        Instant failedAt
) {
}