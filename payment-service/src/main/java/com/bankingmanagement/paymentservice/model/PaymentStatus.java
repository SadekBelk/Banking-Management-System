package com.bankingmanagement.paymentservice.model;

public enum PaymentStatus {

    PENDING,      // Payment created, awaiting processing
    PROCESSING,   // Payment is being processed
    COMPLETED,    // Payment successfully completed
    FAILED,       // Payment failed
    CANCELLED,    // Payment cancelled by user
    REFUNDED      // Payment was refunded

}
