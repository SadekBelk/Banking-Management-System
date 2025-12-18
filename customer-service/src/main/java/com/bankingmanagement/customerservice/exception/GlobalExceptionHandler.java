package com.bankingmanagement.customerservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ==========================================================
       1️⃣ DOMAIN / BUSINESS EXCEPTIONS
       ========================================================== */

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleCustomerNotFound(
            CustomerNotFoundException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Customer Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())       // 🔥 track request path
                .code(ex.getCode())                  // 🔥 use custom error code
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateCustomerException.class)
    public ResponseEntity<ApiError> handleDuplicateCustomer(
            DuplicateCustomerException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Duplicate Resource")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .code(ex.getCode())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleCustomerAlreadyExists(
            CustomerAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())   // 409
                .error("Duplicate Resource")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .code(ex.getCode()) // 🔥 custom error code
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }


    /* ==========================================================
       2️⃣ VALIDATION ERRORS (🔥 Step 6 — VERY IMPORTANT)
       ========================================================== */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));

        ApiError apiError = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .validationErrors(validationErrors)
                .path(request.getRequestURI())
                .code(ApiErrorCode.VALIDATION_FAILED) // 🔥 generic validation error code
                .build();

        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        ApiError apiError = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .code(ApiErrorCode.VALIDATION_FAILED)
                .build();

        return ResponseEntity.badRequest().body(apiError);
    }

    /* ==========================================================
       3️⃣ CATCH-ALL SAFETY NET (🔥 Step 7 — OPTIONAL BUT RECOMMENDED)
       ========================================================== */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllUnhandledExceptions(
            Exception ex,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .code(ApiErrorCode.INTERNAL_ERROR) // 🔥 generic catch-all code
                .build();

        // ⚠️ LOG FULL STACK TRACE
        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
