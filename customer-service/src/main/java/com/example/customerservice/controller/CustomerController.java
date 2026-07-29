package com.example.customerservice.controller;

import com.example.customerservice.dto.CreateCustomerRequest;
import com.example.customerservice.dto.CustomerResponse;
import com.example.customerservice.dto.UpdateCustomerProfileRequest;
import com.example.customerservice.security.PermissionConstants;
import com.example.customerservice.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/me")
    @PreAuthorize(PermissionConstants.HAS_ROLE_USER_OR_ADMIN_OR_SELLER)
    public CustomerResponse getCurrentCustomer(Authentication authentication) {
        return customerService.getCustomerByEmail(authentication.getName());
    }

    @PatchMapping("/me")
    @PreAuthorize(PermissionConstants.HAS_ROLE_USER_OR_ADMIN_OR_SELLER)
    public CustomerResponse updateCurrentCustomer(
            @Valid @RequestBody UpdateCustomerProfileRequest request,
            Authentication authentication
    ) {
        return customerService.updateCustomerProfile(
                authentication.getName(),
                request
        );
    }

    @GetMapping
    @PreAuthorize(PermissionConstants.HAS_ROLE_ADMIN)
    public List<CustomerResponse> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("/{id}")
    @PreAuthorize(PermissionConstants.HAS_ROLE_ADMIN)
    public CustomerResponse getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }
}
