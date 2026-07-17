package com.example.orderservice.exception;

public class InvalidOrderResultEventException
        extends RuntimeException {

    public InvalidOrderResultEventException(
            String message
    ) {
        super(message);
    }
}