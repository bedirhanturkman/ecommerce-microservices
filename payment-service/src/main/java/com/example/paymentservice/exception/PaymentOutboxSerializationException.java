package com.example.paymentservice.exception;

public class PaymentOutboxSerializationException
        extends RuntimeException {

    public PaymentOutboxSerializationException(
            String eventType,
            Throwable cause
    ) {
        super(
                "Payment outbox event could not be serialized. "
                        + "eventType="
                        + eventType,
                cause
        );
    }
}