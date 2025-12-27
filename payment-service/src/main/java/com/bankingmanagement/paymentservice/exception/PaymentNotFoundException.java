package com.bankingmanagement.paymentservice.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(java.util.UUID paymentId) {
        super("Payment not found with id: " + paymentId);
    }

}