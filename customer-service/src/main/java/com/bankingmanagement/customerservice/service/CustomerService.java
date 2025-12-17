package com.bankingmanagement.customerservice.service;

import com.bankingmanagement.customerservice.dto.CustomerRequestDto;
import com.bankingmanagement.customerservice.dto.CustomerResponseDto;
import com.bankingmanagement.customerservice.model.Customer;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    CustomerResponseDto createCustomer(CustomerRequestDto request);

    CustomerResponseDto getCustomerById(UUID customerId);

    List<CustomerResponseDto> getAllCustomers();

    CustomerResponseDto updateCustomer(UUID customerId, CustomerRequestDto request);

    void deleteCustomer(UUID customerId);

}
