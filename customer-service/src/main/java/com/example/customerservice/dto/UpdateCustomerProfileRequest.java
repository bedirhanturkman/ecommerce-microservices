package com.example.customerservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateCustomerProfileRequest(
        @Pattern(
                regexp = ".*\\S.*",
                message = "firstName must not be blank"
        )
        String firstName,

        @Pattern(
                regexp = ".*\\S.*",
                message = "lastName must not be blank"
        )
        String lastName
) {
}
