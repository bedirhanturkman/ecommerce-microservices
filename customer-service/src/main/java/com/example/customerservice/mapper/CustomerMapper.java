package com.example.customerservice.mapper;

import com.example.customerservice.dto.CreateCustomerRequest;
import com.example.customerservice.dto.CustomerInternalResponse;
import com.example.customerservice.dto.CustomerResponse;
import com.example.customerservice.entity.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toCustomerResponse(Customer customer);

    CustomerInternalResponse toCustomerInternalResponse(Customer customer);

    Customer toCustomer(CreateCustomerRequest request);
}