package com.example.commonevents.inventory;

public record ReservedInventoryItem(
        String productId,
        Integer quantity
) {
}