package com.example.inventoryservice.exception;

public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(String productId) {
        super("Inventory not found for product: " + productId);
    }
}