package com.example.customerservice.dto;

public record CustomerRegistrationResponse(
        Long id,
        String email,
        String role
) {
}
