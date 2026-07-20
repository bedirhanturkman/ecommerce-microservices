package com.example.inventoryservice.exception;

public class InventoryOutboxSerializationException
        extends RuntimeException {

    public InventoryOutboxSerializationException(
            String eventType,
            Throwable cause
    ) {
        super(
                "Inventory outbox event could not be serialized. "
                        + "eventType="
                        + eventType,
                cause
        );
    }
}