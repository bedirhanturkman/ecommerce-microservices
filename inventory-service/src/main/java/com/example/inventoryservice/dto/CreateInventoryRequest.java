package com.example.inventoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateInventoryRequest(

        @NotBlank(message = "Product id cannot be blank")
        String productId,

        @NotNull(message = "Quantity is required")
        @PositiveOrZero(message = "Quantity cannot be negative") // Burada ilk stok değeri 0 olabilir. Bu nedenle @PositiveOrZero kullanıyoruz.
        Integer quantity
) {
}