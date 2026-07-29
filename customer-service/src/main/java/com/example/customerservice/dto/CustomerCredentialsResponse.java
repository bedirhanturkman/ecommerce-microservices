package com.example.customerservice.dto;

public record CustomerCredentialsResponse(
        Long id,
        String email,
        String passwordHash,
        String role
) {
}
