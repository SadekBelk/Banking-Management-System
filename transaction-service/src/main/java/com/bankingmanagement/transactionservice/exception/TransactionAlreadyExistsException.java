package com.bankingmanagement.transactionservice.exception;

public class TransactionAlreadyExistsException extends TransactionException {
    public TransactionAlreadyExistsException(String message) {
        super(message);
    }
}
