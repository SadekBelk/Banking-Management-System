package com.bankingmanagement.customerservice.controller;

import com.bankingmanagement.customerservice.dto.CustomerRequestDto;
import com.bankingmanagement.customerservice.dto.CustomerResponseDto;
import com.bankingmanagement.customerservice.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Create Customer", description = "Creates a new customer")
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @Valid @RequestBody CustomerRequestDto request
    ) {
        CustomerResponseDto created = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------------------- GET CUSTOMER BY ID --------------------
    @GetMapping("/{id}")
    @Operation(summary = "Get Customer by ID", description = "Retrieves a customer by their unique ID")
    public ResponseEntity<CustomerResponseDto> getCustomerById(
            @PathVariable("id") UUID id
    ) {
        CustomerResponseDto customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    // -------------------- GET ALL CUSTOMERS --------------------
    @GetMapping
    @Operation(summary = "Get All Customers", description = "Retrieves a list of all customers")
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        List<CustomerResponseDto> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    // -------------------- UPDATE CUSTOMER --------------------
    @PutMapping("/{id}")
    @Operation(summary = "Update Customer", description = "Updates an existing customer's information")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CustomerRequestDto request
    ) {
        CustomerResponseDto updated = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(updated);
    }

    // -------------------- DELETE CUSTOMER --------------------
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Customer", description = "Deletes a customer by their unique ID")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("id") UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

}
