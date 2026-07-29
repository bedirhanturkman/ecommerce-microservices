package com.example.orderservice.security;

public record OrderAccessContext(
        Long customerId,
        boolean admin
) {
}
