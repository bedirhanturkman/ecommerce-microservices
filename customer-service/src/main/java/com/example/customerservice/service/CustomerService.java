package com.example.customerservice.service;

import com.example.customerservice.dto.CreateCustomerRequest;
import com.example.customerservice.dto.CustomerInternalResponse;
import com.example.customerservice.dto.CustomerResponse;
import com.example.customerservice.entity.Customer;
import com.example.customerservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return mapToResponse(customer);
    }

    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return mapToResponse(customer);
    }

    public CustomerInternalResponse createCustomerInternal(CreateCustomerRequest request) {

        if (customerRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }

        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(request.password())
                .role(request.role())
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        return mapToInternalResponse(savedCustomer);
    }

    public CustomerInternalResponse getCustomerInternalByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return mapToInternalResponse(customer);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail()
        );
    }

    private CustomerInternalResponse mapToInternalResponse(Customer customer) {
        return new CustomerInternalResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPassword(),
                customer.getRole()
        );
    }
}