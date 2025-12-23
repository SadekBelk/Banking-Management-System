package com.bankingmanagement.transactionservice.exception;

public class InsufficientFundsException extends TransactionException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
