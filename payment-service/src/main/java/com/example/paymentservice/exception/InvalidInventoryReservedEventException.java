package com.example.paymentservice.exception;

public class InvalidInventoryReservedEventException
        extends RuntimeException {

    public InvalidInventoryReservedEventException(
            String message
    ) {
        super(message);
    }
}