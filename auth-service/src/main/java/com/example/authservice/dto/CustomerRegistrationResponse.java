package com.example.authservice.dto;

public record CustomerRegistrationResponse(
        Long id,
        String email,
        String role
) {
}
