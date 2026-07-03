package com.example.customerservice.controller;

import com.example.customerservice.dto.CreateCustomerRequest;
import com.example.customerservice.dto.CustomerInternalResponse;
import com.example.customerservice.dto.CustomerResponse;
import com.example.customerservice.service.CustomerService;
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
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CustomerResponse getCurrentCustomer(Authentication authentication) {
        return customerService.getCustomerByEmail(authentication.getName());
    }

    @PostMapping("/internal")
    public CustomerInternalResponse createCustomerInternal(@RequestBody CreateCustomerRequest request) {
        return customerService.createCustomerInternal(request);
    }

    @GetMapping("/by-email/{email}")
    public CustomerInternalResponse getCustomerByEmailInternal(@PathVariable String email) {
        return customerService.getCustomerInternalByEmail(email);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CustomerResponse> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }
}