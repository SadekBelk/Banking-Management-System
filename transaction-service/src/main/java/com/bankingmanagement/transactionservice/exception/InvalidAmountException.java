package com.bankingmanagement.transactionservice.exception;

public class InvalidAmountException extends TransactionException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
