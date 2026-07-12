package com.example.commonevents.order;

import java.math.BigDecimal;

public record OrderCreatedItemEvent(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}