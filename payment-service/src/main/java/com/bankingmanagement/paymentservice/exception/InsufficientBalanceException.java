package com.bankingmanagement.paymentservice.exception;

/**
 * Exception thrown when there is insufficient balance to complete an operation.
 * This is thrown by the gRPC client when account-service returns FAILED_PRECONDITION
 * due to insufficient available balance.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
