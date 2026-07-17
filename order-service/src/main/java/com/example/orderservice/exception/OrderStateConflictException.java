package com.example.orderservice.exception;

import com.example.orderservice.entity.OrderStatus;

public class OrderStateConflictException
        extends RuntimeException {

    public OrderStateConflictException(
            Long orderId,
            OrderStatus currentStatus,
            OrderStatus requestedStatus
    ) {
        super(
                "Order state conflict. orderId="
                        + orderId
                        + ", currentStatus="
                        + currentStatus
                        + ", requestedStatus="
                        + requestedStatus
        );
    }
}