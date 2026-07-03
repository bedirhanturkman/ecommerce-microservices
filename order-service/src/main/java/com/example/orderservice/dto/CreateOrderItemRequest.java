package com.example.orderservice.dto;

public record CreateOrderItemRequest(
        String productId,
        Integer quantity
) {
}