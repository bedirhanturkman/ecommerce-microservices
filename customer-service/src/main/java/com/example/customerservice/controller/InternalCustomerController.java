package com.example.customerservice.controller;

import com.example.customerservice.dto.CreateCustomerRequest;
import com.example.customerservice.dto.CustomerCredentialsResponse;
import com.example.customerservice.dto.CustomerRegistrationResponse;
import com.example.customerservice.security.InternalApiKeyValidator;
import com.example.customerservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/customers")
@RequiredArgsConstructor
public class InternalCustomerController {

    public static final String INTERNAL_API_KEY_HEADER =
            "X-Internal-Api-Key";

    private final CustomerService customerService;
    private final InternalApiKeyValidator internalApiKeyValidator;

    @PostMapping
    public CustomerRegistrationResponse createCustomer(
            @RequestHeader(
                    value = INTERNAL_API_KEY_HEADER,
                    required = false
            ) String internalApiKey,
            @RequestBody CreateCustomerRequest request
    ) {
        internalApiKeyValidator.validate(internalApiKey);
        return customerService.createCustomerInternal(request);
    }

    @GetMapping("/credentials/by-email/{email}")
    public CustomerCredentialsResponse getCredentialsByEmail(
            @RequestHeader(
                    value = INTERNAL_API_KEY_HEADER,
                    required = false
            ) String internalApiKey,
            @PathVariable String email
    ) {
        internalApiKeyValidator.validate(internalApiKey);
        return customerService.getCustomerCredentialsByEmail(email);
    }
}
