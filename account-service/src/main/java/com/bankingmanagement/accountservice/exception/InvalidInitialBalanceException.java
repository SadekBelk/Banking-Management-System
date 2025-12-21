package com.bankingmanagement.accountservice.exception;

public class InvalidInitialBalanceException extends RuntimeException{

    public InvalidInitialBalanceException(String message) {
        super(message);
    }

}
