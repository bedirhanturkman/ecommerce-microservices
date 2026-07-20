package com.example.orderservice.exception;

public class OrderOutboxSerializationException
        extends RuntimeException {

    public OrderOutboxSerializationException(
            String eventType,
            Throwable cause
    ) {
        super(
                "Order outbox event could not be serialized. "
                        + "eventType="
                        + eventType,
                cause
        );
    }
}