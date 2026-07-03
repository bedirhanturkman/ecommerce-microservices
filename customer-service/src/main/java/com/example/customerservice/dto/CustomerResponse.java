package com.example.customerservice.dto;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) {}