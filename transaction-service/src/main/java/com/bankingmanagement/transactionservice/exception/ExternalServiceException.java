package com.bankingmanagement.transactionservice.exception;

public class ExternalServiceException extends TransactionException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
