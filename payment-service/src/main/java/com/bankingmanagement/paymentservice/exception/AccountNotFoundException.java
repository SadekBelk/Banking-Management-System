package com.bankingmanagement.paymentservice.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }

    public AccountNotFoundException(java.util.UUID accountId) {
        super("Account not found with id: " + accountId);
    }

}