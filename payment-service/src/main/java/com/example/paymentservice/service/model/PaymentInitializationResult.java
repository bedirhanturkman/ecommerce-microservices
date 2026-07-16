package com.example.paymentservice.service.model;

public record PaymentInitializationResult(
        Long paymentId,
        Long orderId,
        boolean created
) {
}