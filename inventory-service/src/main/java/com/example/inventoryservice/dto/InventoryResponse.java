package com.example.inventoryservice.dto;

import java.time.Instant;

public record InventoryResponse(
        String id,
        String productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity, // availableQuantity MongoDB’de ayrı alan olarak tutulmayacak: Response oluşturulurken hesaplanacak. availableQuantity = quantity - reservedQuantity
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}