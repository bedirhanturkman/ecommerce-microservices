package com.example.orderservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record OrderSagaResponse(
        SagaOrder order,
        SagaInventory inventory,
        SagaPayment payment,
        String sagaStatus,
        boolean projectionAvailable,
        Instant lastUpdatedAt
) {
    public record SagaOrder(
            Long orderId,
            Long customerId,
            String status,
            List<OrderItemResponse> items,
            BigDecimal totalAmount,
            LocalDateTime createdAt,
            Long version
    ) {
    }

    public record SagaInventory(
            String status,
            String failureCode,
            String failureMessage,
            List<SagaReservation> reservations
    ) {
    }

    public record SagaReservation(
            Long reservationId,
            Long orderId,
            String productId,
            Integer quantity,
            String status,
            Long version
    ) {
    }

    public record SagaPayment(
            boolean exists,
            Long paymentId,
            Long orderId,
            BigDecimal amount,
            String status,
            String failureCode,
            String failureReason
    ) {
    }
}
