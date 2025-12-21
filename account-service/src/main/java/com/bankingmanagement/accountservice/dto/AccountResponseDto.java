package com.bankingmanagement.accountservice.dto;

import com.bankingmanagement.accountservice.model.AccountStatus;
import com.bankingmanagement.accountservice.model.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AccountResponseDto {

    private UUID id;
    private String accountNumber;

    private UUID customerId;
    private AccountType type;
    private AccountStatus status;

    private BigDecimal balance;

    private Instant createdAt;
    private Instant updatedAt;

    /* =========================
       Getters & Setters
       ========================= */

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
