package com.example.productservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @Pattern(
                regexp = ".*\\S.*",
                message = "name must not be blank"
        )
        String name,

        String description,

        @Positive(message = "price must be greater than zero")
        BigDecimal price
) {
}
