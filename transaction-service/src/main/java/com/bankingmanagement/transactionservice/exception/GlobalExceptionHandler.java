package com.bankingmanagement.transactionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /* =========================
       DOMAIN EXCEPTIONS
       ========================= */

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TransactionNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ApiErrorCode.TRANSACTION_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransactionAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleAlreadyExists(TransactionAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ApiErrorCode.TRANSACTION_ALREADY_EXISTS, ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException ex) {
        return build(HttpStatus.BAD_REQUEST, ApiErrorCode.INSUFFICIENT_FUNDS, ex.getMessage());
    }

    @ExceptionHandler(SameAccountTransferException.class)
    public ResponseEntity<ApiError> handleSameAccount(SameAccountTransferException ex) {
        return build(HttpStatus.BAD_REQUEST, ApiErrorCode.SAME_ACCOUNT_TRANSFER, ex.getMessage());
    }

    @ExceptionHandler(InvalidTransactionTypeException.class)
    public ResponseEntity<ApiError> handleInvalidType(InvalidTransactionTypeException ex) {
        return build(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_TRANSACTION_TYPE, ex.getMessage());
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ApiError> handleInvalidAmount(InvalidAmountException ex) {
        return build(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_AMOUNT, ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidStatus(InvalidStatusTransitionException ex) {
        return build(HttpStatus.CONFLICT, ApiErrorCode.INVALID_STATUS_TRANSITION, ex.getMessage());
    }

    @ExceptionHandler(TransactionProcessingException.class)
    public ResponseEntity<ApiError> handleProcessing(TransactionProcessingException ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.TRANSACTION_PROCESSING_FAILED, ex.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiError> handleExternal(ExternalServiceException ex) {
        return build(HttpStatus.BAD_GATEWAY, ApiErrorCode.EXTERNAL_SERVICE_ERROR, ex.getMessage());
    }

    /* =========================
       VALIDATION EXCEPTIONS
       ========================= */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = (fieldError != null) ? fieldError.getDefaultMessage() : "Validation failed";
        return build(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Validation failed";
        return build(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, message);
    }

    /* =========================
       FALLBACK
       ========================= */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ApiErrorCode code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(code, message, status.value()));
    }
}
