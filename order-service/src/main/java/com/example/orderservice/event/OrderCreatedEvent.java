package com.example.orderservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt,
        List<OrderCreatedItemEvent> items
) {
}