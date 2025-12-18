package com.bankingmanagement.customerservice.controller;

import com.bankingmanagement.customerservice.dto.CustomerRequestDto;
import com.bankingmanagement.customerservice.dto.CustomerResponseDto;
import com.bankingmanagement.customerservice.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "API for managing CUSTOMERS")
public class CustomerController {

    private final CustomerService customerService;

    // -------------------- CREATE CUSTOMER --------------------
    @PostMapping
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @Valid @RequestBody CustomerRequestDto request
    ) {
        CustomerResponseDto created = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------------------- GET CUSTOMER BY ID --------------------
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getCustomerById(
            @PathVariable("id") UUID id
    ) {
        CustomerResponseDto customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    // -------------------- GET ALL CUSTOMERS --------------------
    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        List<CustomerResponseDto> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    // -------------------- UPDATE CUSTOMER --------------------
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CustomerRequestDto request
    ) {
        CustomerResponseDto updated = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(updated);
    }

    // -------------------- DELETE CUSTOMER --------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("id") UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

}
