package com.bankingmanagement.paymentservice.model;

public enum PaymentType {

    TRANSFER,          // Internal transfer between accounts
    BILL_PAYMENT,      // Bill payment
    MERCHANT_PAYMENT,  // Payment to a merchant
    P2P,               // Person-to-person payment
    REFUND             // Refund payment

}
