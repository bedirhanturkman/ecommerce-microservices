package com.example.inventoryservice.exception;

public class InventoryAlreadyExistsException extends RuntimeException {

    public InventoryAlreadyExistsException(String productId) {
        super("Inventory already exists for product: " + productId);
    }
}