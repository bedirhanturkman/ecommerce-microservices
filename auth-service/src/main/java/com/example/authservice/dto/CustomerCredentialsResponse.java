package com.example.authservice.dto;

public record CustomerCredentialsResponse(
        Long id,
        String email,
        String passwordHash,
        String role
) {
}
