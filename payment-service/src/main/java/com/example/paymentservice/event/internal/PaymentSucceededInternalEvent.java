package com.example.paymentservice.event.internal;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSucceededInternalEvent(
        Long paymentId,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        Instant succeededAt
) {
}