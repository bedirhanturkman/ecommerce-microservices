package com.example.orderservice.saga;

public enum InventorySagaStatus {
    PENDING,
    RESERVED,
    CONFIRMED,
    RELEASED,
    FAILED
}
