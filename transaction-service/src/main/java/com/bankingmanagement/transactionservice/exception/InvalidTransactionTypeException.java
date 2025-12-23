package com.bankingmanagement.transactionservice.exception;

public class InvalidTransactionTypeException extends TransactionException {
    public InvalidTransactionTypeException(String message) {
        super(message);
    }
}
