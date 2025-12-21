package com.bankingmanagement.accountservice.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(UUID customerId) {
        super("Customer not found with id: " + customerId);
    }

}
