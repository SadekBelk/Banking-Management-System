package com.bankingmanagement.transactionservice.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Standard API error payload to keep responses consistent across services.
 */
public class ApiError {

    private final ApiErrorCode code;
    private final String message;
    private final int status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    public ApiError(ApiErrorCode code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
