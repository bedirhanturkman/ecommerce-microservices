package com.example.orderservice.exception;

public class OrderResultMismatchException
        extends RuntimeException {

    public OrderResultMismatchException(
            Long orderId,
            String message
    ) {
        super(
                "Order result event does not match order. "
                        + "orderId="
                        + orderId
                        + ", reason="
                        + message
        );
    }
}