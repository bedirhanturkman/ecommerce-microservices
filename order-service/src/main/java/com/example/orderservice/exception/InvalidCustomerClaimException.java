package com.example.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class InvalidCustomerClaimException extends RuntimeException {

    public InvalidCustomerClaimException() {
        super("A valid customerId claim is required");
    }
}
