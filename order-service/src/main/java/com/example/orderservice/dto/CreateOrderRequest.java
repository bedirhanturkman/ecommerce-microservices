package com.example.orderservice.dto;

import java.util.List;

public record CreateOrderRequest(
        Long customerId,
        List<CreateOrderItemRequest> items
) {
}