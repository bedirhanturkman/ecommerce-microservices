package com.example.authservice.dto;

public record CustomerInternalResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String password,
        String role
) {
}