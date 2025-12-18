package com.bankingmanagement.customerservice.service.impl;

import com.bankingmanagement.customerservice.dto.CustomerRequestDto;
import com.bankingmanagement.customerservice.dto.CustomerResponseDto;
import com.bankingmanagement.customerservice.exception.CustomerAlreadyExistsException;
import com.bankingmanagement.customerservice.exception.CustomerNotFoundException;
import com.bankingmanagement.customerservice.exception.InvalidKycTransitionException;
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
            throw new CustomerAlreadyExistsException(request.getEmail());
        }

        Customer customer = customerMapper.toEntity(request);

        // Default KYC status
        customer.setKycStatus(
                request.getKycStatus() != null ? request.getKycStatus() : KycStatus.PENDING
        );

        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .map(customerMapper::toResponseDto)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
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
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        // Email immutability protection
        if (!customer.getEmail().equals(request.getEmail())
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException(request.getEmail());
        }

        // KYC transition validation
        validateKycTransition(customer.getKycStatus(), request.getKycStatus());

        customerMapper.updateEntityFromDto(request, customer);

        Customer updated = customerRepository.save(customer);
        return customerMapper.toResponseDto(updated);
    }

    @Override
    public void deleteCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        customerRepository.delete(customer);
    }

    // -------------------- DOMAIN RULES --------------------

    private void validateKycTransition(KycStatus current, KycStatus target) {
        if (target == null || current == target) return;

        if (current == KycStatus.VERIFIED && target != KycStatus.VERIFIED) {
            throw new InvalidKycTransitionException(current, target);
        }
    }
}
