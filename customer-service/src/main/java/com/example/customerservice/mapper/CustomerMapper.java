package com.example.customerservice.mapper;

import com.example.customerservice.dto.CreateCustomerRequest;
import com.example.customerservice.dto.CustomerCredentialsResponse;
import com.example.customerservice.dto.CustomerRegistrationResponse;
import com.example.customerservice.dto.CustomerResponse;
import com.example.customerservice.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toCustomerResponse(Customer customer);

    CustomerRegistrationResponse toCustomerRegistrationResponse(
            Customer customer
    );

    @Mapping(source = "password", target = "passwordHash")
    CustomerCredentialsResponse toCustomerCredentialsResponse(
            Customer customer
    );

    Customer toCustomer(CreateCustomerRequest request);
}
