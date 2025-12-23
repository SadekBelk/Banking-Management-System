package com.bankingmanagement.transactionservice.exception;

public class TransactionProcessingException extends TransactionException {
    public TransactionProcessingException(String message) {
        super(message);
    }

    public TransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
