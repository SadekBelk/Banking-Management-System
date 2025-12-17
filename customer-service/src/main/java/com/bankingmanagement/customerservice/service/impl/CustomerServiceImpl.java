package com.bankingmanagement.customerservice.service.impl;

import com.bankingmanagement.customerservice.dto.CustomerRequestDto;
import com.bankingmanagement.customerservice.dto.CustomerResponseDto;
import com.bankingmanagement.customerservice.mapper.CustomerMapper;
import com.bankingmanagement.customerservice.model.Customer;
import com.bankingmanagement.customerservice.model.KycStatus;
import com.bankingmanagement.customerservice.repository.CustomerRepository;
import com.bankingmanagement.customerservice.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponseDto createCustomer(CustomerRequestDto request) {

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Customer with email already exists: " + request.getEmail()
            );
        }

        Customer customer = customerMapper.toEntity(request);

        if (customer.getKycStatus() == null) {
            customer.setKycStatus(KycStatus.PENDING);
        }

        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found: " + customerId
                ));
        return customerMapper.toResponseDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(customerMapper::toResponseDto)
                .toList();
    }

    @Override
    public CustomerResponseDto updateCustomer(UUID customerId, CustomerRequestDto request) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found: " + customerId
                ));

        customerMapper.updateEntityFromDto(request, customer);

        Customer updated = customerRepository.save(customer);
        return customerMapper.toResponseDto(updated);
    }

    @Override
    public void deleteCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found: " + customerId
                ));
        customerRepository.delete(customer);
    }

}
