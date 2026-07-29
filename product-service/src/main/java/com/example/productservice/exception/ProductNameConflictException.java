package com.example.productservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProductNameConflictException extends RuntimeException {

    public ProductNameConflictException(String name) {
        super("Product name already exists: " + name);
    }
}
