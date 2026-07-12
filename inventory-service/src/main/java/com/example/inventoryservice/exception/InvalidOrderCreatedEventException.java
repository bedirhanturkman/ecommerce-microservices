package com.example.inventoryservice.exception;

public class InvalidOrderCreatedEventException extends RuntimeException {

    public InvalidOrderCreatedEventException(String message) {
        super(message);
    }
}