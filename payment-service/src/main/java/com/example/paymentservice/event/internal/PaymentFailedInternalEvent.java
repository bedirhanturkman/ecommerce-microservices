package com.example.paymentservice.event.internal;

import com.example.commonevents.payment.PaymentFailureCode;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentFailedInternalEvent(
        Long paymentId,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        PaymentFailureCode failureCode,
        String failureReason,
        Instant failedAt
) {
}