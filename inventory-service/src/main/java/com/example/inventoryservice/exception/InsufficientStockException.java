package com.example.inventoryservice.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(
            String productId,
            int availableQuantity,
            int requestedQuantity
    ) {
        super(
                "Insufficient stock for product '%s'. Available: %d, requested: %d"
                        .formatted(
                                productId,
                                availableQuantity,
                                requestedQuantity
                        )
        );
    }
}