package com.example.commonevents.inventory;

public record InventoryReservationItem(
        Long reservationId,
        String productId,
        Integer quantity,
        String status,
        Long version
) {
}
