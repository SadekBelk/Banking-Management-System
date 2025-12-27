package com.bankingmanagement.accountservice.controller;


import com.bankingmanagement.accountservice.dto.AccountRequestDto;
import com.bankingmanagement.accountservice.dto.AccountResponseDto;
import com.bankingmanagement.accountservice.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "API for managing banking ACCOUNTS")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ✅ Create account
    @PostMapping
    @Operation(summary = "Create Account", description = "Creates a new bank account")
    public ResponseEntity<AccountResponseDto> createAccount(
            @Valid @RequestBody AccountRequestDto request
    ) {
        AccountResponseDto response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ Get account by id
    @GetMapping("/{id}")
    @Operation(summary = "Get Account by ID", description = "Retrieves a bank account by its unique ID")
    public ResponseEntity<AccountResponseDto> getAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    // ✅ Get all accounts for a customer
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all accounts for a customer", description = "Retrieves all bank accounts associated with a specific customer")
    public ResponseEntity<List<AccountResponseDto>> getAccountsByCustomer(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    // ✅ Close account
    @PatchMapping("/{id}/close")
    @Operation(summary = "Close Account", description = "Deactivates (closes) a bank account")
    public ResponseEntity<Void> closeAccount(@PathVariable UUID id) {
        accountService.deactivateAccount(id);
        return ResponseEntity.noContent().build();
    }

}
