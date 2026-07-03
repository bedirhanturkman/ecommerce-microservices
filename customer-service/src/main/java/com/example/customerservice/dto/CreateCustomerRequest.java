package com.example.customerservice.dto;

public record CreateCustomerRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        String role
) {
}