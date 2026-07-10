package com.example.orderservice.event;

import java.math.BigDecimal;

public record OrderCreatedItemEvent(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}