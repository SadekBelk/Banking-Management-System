package com.bankingmanagement.customerservice.exception;

public class DuplicateCustomerException extends RuntimeException {

    private final ApiErrorCode code = ApiErrorCode.CUSTOMER_ALREADY_EXISTS;

    public DuplicateCustomerException(String message) {
        super("Customer with email already exists: " + message);
    }

    public ApiErrorCode getCode() {
        return code;
    }

}
