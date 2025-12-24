package com.bankingmanagement.transactionservice.exception;

public class InvalidStatusTransitionException extends TransactionException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
