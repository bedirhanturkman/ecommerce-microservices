package com.example.inventoryservice.exception;

public class InvalidStockQuantityException extends RuntimeException {

    public InvalidStockQuantityException(String message) {
        super(message);
    }
}