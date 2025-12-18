package com.bankingmanagement.customerservice.exception;

import com.bankingmanagement.customerservice.model.KycStatus;

public class InvalidKycTransitionException extends RuntimeException {

    private final ApiErrorCode code = ApiErrorCode.INVALID_KYC_TRANSITION;

    public InvalidKycTransitionException(KycStatus from, KycStatus to) {
        super("Invalid KYC transition from " + from + " to " + to);
    }

    public ApiErrorCode getCode() {
        return code;
    }

}