package com.bankingmanagement.customerservice.exception;

public class CustomerAlreadyExistsException extends RuntimeException {

    private final ApiErrorCode code = ApiErrorCode.CUSTOMER_ALREADY_EXISTS;

    public CustomerAlreadyExistsException(String email) {
        super("Customer with email already exists: " + email);
    }

    public ApiErrorCode getCode() {
        return code;
    }

}