package com.example.inventoryservice.exception;

public class InvalidPaymentResultEventException
        extends RuntimeException {

    public InvalidPaymentResultEventException(
            String message
    ) {
        super(message);
    }
}