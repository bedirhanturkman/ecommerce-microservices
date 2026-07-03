package com.example.authservice.dto;

public record CreateCustomerRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        String role
) {
}