package com.example.paymentservice.exception;

public class PaymentAlreadyExistsException
        extends RuntimeException {

    public PaymentAlreadyExistsException(
            Long orderId
    ) {
        super(
                "Payment already exists for order: "
                        + orderId
        );
    }
}