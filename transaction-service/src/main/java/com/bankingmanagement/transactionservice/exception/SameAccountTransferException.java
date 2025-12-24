package com.bankingmanagement.transactionservice.exception;

public class SameAccountTransferException extends TransactionException {
    public SameAccountTransferException(String message) {
        super(message);
    }
}
