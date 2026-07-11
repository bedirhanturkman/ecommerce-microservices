package com.example.inventoryservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockChangeRequest(

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero") // Stok artırma veya azaltma miktarı 0 olamayacağı için @Positive kullanıyoruz.
        Integer quantity
) {
}