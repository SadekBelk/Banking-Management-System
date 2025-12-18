package com.bankingmanagement.customerservice.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

    private final ApiErrorCode code = ApiErrorCode.CUSTOMER_NOT_FOUND;

    public CustomerNotFoundException(UUID id) {
        super("Customer not found: " + id);
    }

    public ApiErrorCode getCode() {
        return code;
    }
}
