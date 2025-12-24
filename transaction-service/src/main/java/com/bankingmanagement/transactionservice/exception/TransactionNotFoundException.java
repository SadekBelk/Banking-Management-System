package com.bankingmanagement.transactionservice.exception;

public class TransactionNotFoundException extends TransactionException {
    
    public TransactionNotFoundException(String message) {
        super(message);
    }
    
    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
