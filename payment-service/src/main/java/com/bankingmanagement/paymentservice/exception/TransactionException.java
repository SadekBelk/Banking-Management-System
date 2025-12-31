package com.bankingmanagement.paymentservice.exception;

/**
 * Exception thrown when there's an error with the transaction-service.
 */
public class TransactionException extends RuntimeException {
    
    public TransactionException(String message) {
        super(message);
    }
    
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
