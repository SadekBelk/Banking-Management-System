package com.bankingmanagement.transactionservice.model;

/**
 * Types of transactions supported by the banking system.
 */
public enum TransactionType {
    TRANSFER,       // Account to account transfer
    DEPOSIT,        // Add funds to account
    WITHDRAWAL,     // Remove funds from account
    BILL_PAYMENT,   // Payment to a biller/merchant
    P2P,            // Person to person payment
    REFUND          // Refund of a previous transaction
}
