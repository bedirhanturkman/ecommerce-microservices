package com.example.commonevents.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSucceededEvent(
        Long paymentId,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        Instant succeededAt
) {
}