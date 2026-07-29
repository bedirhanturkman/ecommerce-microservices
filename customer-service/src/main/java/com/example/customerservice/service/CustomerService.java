package com.example.customerservice.service;

import com.example.customerservice.dto.CreateCustomerRequest;
import com.example.customerservice.dto.CustomerCredentialsResponse;
import com.example.customerservice.dto.CustomerRegistrationResponse;
import com.example.customerservice.dto.CustomerResponse;
import com.example.customerservice.dto.UpdateCustomerProfileRequest;
import com.example.customerservice.entity.Customer;
import com.example.customerservice.exception.CustomerNotFoundException;
import com.example.customerservice.mapper.CustomerMapper;
import com.example.customerservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper
    ) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(customerMapper::toCustomerResponse)
                .toList();
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return customerMapper.toCustomerResponse(customer);
    }

    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return customerMapper.toCustomerResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomerProfile(
            String email,
            UpdateCustomerProfileRequest request
    ) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException(email));

        if (request.firstName() != null) {
            customer.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            customer.setLastName(request.lastName());
        }

        Customer savedCustomer = customerRepository.save(customer);

        return customerMapper.toCustomerResponse(savedCustomer);
    }

    public CustomerRegistrationResponse createCustomerInternal(
            CreateCustomerRequest request
    ) {

        if (customerRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }

        Customer customer = customerMapper.toCustomer(request);

        Customer savedCustomer = customerRepository.save(customer);

        return customerMapper.toCustomerRegistrationResponse(savedCustomer);
    }

    public CustomerCredentialsResponse getCustomerCredentialsByEmail(
            String email
    ) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return customerMapper.toCustomerCredentialsResponse(customer);
    }
}
