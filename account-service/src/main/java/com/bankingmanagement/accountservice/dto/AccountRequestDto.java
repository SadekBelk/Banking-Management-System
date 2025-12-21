package com.bankingmanagement.accountservice.dto;

import com.bankingmanagement.accountservice.model.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountRequestDto {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Account type is required")
    private AccountType type;

    @Positive(message = "Initial balance must be positive")
    private BigDecimal initialBalance;

    // 🔒 Status is NOT allowed from client
    // 🔒 Account number is NOT allowed from client

    private String currency;

    /* =========================
       Getters & Setters
       ========================= */

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

}
