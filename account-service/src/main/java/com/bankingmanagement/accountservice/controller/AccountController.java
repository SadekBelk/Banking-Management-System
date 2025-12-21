package com.bankingmanagement.accountservice.controller;


import com.bankingmanagement.accountservice.dto.AccountRequestDto;
import com.bankingmanagement.accountservice.dto.AccountResponseDto;
import com.bankingmanagement.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ✅ Create account
    @PostMapping
    public ResponseEntity<AccountResponseDto> createAccount(
            @Valid @RequestBody AccountRequestDto request
    ) {
        AccountResponseDto response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ Get account by id
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDto> getAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    // ✅ Get all accounts for a customer
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponseDto>> getAccountsByCustomer(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    // ✅ Close account
    @PatchMapping("/{id}/close")
    public ResponseEntity<Void> closeAccount(@PathVariable UUID id) {
        accountService.deactivateAccount(id);
        return ResponseEntity.noContent().build();
    }

}
