package com.bankingmanagement.authservice.exception;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                           GLOBAL EXCEPTION HANDLER                                        ║
║                                                                                           ║
║  BUILD ORDER: STEP 6f of 12 (Final Exception Component)                                  ║
║  PREVIOUS STEP: ApiError (response format)                                               ║
║  NEXT STEP: Security components (Step 7)                                                 ║
║                                                                                           ║
║  WHAT THIS CLASS DOES:                                                                   ║
║  - CATCHES all exceptions thrown by controllers/services                                 ║
║  - CONVERTS them to standardized ApiError responses                                      ║
║  - ENSURES no stack traces leak to clients (security)                                    ║
║  - LOGS errors for debugging                                                             ║
║                                                                                           ║
║  WHY THIS IS NEEDED:                                                                     ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  WITHOUT GlobalExceptionHandler:                                                    │ ║
║  │  - Spring returns ugly JSON with stack traces                                       │ ║
║  │  - Different exceptions = different response formats                                │ ║
║  │  - Internal details exposed to attackers                                            │ ║
║  │                                                                                     │ ║
║  │  WITH GlobalExceptionHandler:                                                       │ ║
║  │  - Consistent error format every time                                               │ ║
║  │  - Controlled information exposure                                                  │ ║
║  │  - Proper logging for debugging                                                     │ ║
║  │  - HTTP status codes set correctly                                                  │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  HOW IT WORKS:                                                                           ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  1. Controller/Service throws exception                                             │ ║
║  │                                                                                     │ ║
║  │  2. Spring looks for @ExceptionHandler that matches                                 │ ║
║  │     (Most specific match wins - InvalidTokenException before AuthException)         │ ║
║  │                                                                                     │ ║
║  │  3. Handler method converts exception to ApiError                                   │ ║
║  │                                                                                     │ ║
║  │  4. Spring serializes ApiError to JSON and returns to client                        │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  EXCEPTION HANDLING ORDER:                                                               ║
║  - InvalidTokenException (most specific - caught first)                                  ║
║  - AuthException (parent class - caught if no specific handler)                          ║
║  - Spring Security exceptions (BadCredentialsException, LockedException, etc.)           ║
║  - AccessDeniedException (403 Forbidden)                                                 ║
║  - MethodArgumentNotValidException (@Valid failures)                                     ║
║  - Exception (catch-all for unexpected errors)                                           ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Global exception handler for the auth-service.
 * Converts exceptions to standardized API error responses.
 * 
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * - Applies to ALL controllers in this application
 * - Returns JSON (not view name)
 * 
 * @Slf4j = Lombok annotation that creates a 'log' field
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========================= OUR CUSTOM EXCEPTIONS =========================
    /*
     * These handlers catch our application-specific exceptions.
     * More specific exceptions must come BEFORE their parent classes.
     */

    /**
     * Handle AuthException (our base authentication exception).
     * 
     * Catches: AuthException and its subclasses (if no more specific handler)
     * Returns: HTTP 401 Unauthorized
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuthException(AuthException ex, HttpServletRequest request) {
        log.warn("Authentication error: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle InvalidTokenException specifically.
     * 
     * WHY SEPARATE FROM AuthException?
     * - More specific logging
     * - Could add special handling (e.g., clear cookies)
     * - Shows in code that token errors are expected/handled
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidTokenException(InvalidTokenException ex, HttpServletRequest request) {
        log.warn("Invalid token: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle ResourceNotFoundException.
     * 
     * Returns: HTTP 404 Not Found
     * Different status than auth errors (404 vs 401)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .code(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ========================= SPRING SECURITY EXCEPTIONS =========================
    /*
     * Spring Security throws these during authentication.
     * We catch them to provide our standardized error format.
     * 
     * These come from AuthenticationManager.authenticate() when:
     * - Password is wrong (BadCredentialsException)
     * - Account is disabled (DisabledException)
     * - Account is locked (LockedException)
     * - etc.
     */

    /**
     * Wrong password/username.
     * 
     * SECURITY NOTE: We return generic "Invalid credentials" message,
     * NOT "User not found" or "Wrong password" - prevents username enumeration.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(AuthErrorCode.INVALID_CREDENTIALS.getCode())
                .message(AuthErrorCode.INVALID_CREDENTIALS.getDefaultMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Account is disabled by admin (User.enabled = false).
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabledException(DisabledException ex, HttpServletRequest request) {
        log.warn("Account disabled: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(AuthErrorCode.ACCOUNT_DISABLED.getCode())
                .message(AuthErrorCode.ACCOUNT_DISABLED.getDefaultMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Account is locked (User.accountNonLocked = false).
     * Usually due to too many failed login attempts.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiError> handleLockedException(LockedException ex, HttpServletRequest request) {
        log.warn("Account locked: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(AuthErrorCode.ACCOUNT_LOCKED.getCode())
                .message(AuthErrorCode.ACCOUNT_LOCKED.getDefaultMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Account validity period has ended (User.accountNonExpired = false).
     */
    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<ApiError> handleAccountExpiredException(AccountExpiredException ex, HttpServletRequest request) {
        log.warn("Account expired: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(AuthErrorCode.ACCOUNT_EXPIRED.getCode())
                .message(AuthErrorCode.ACCOUNT_EXPIRED.getDefaultMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Password has expired (User.credentialsNonExpired = false).
     * User must change password before continuing.
     */
    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ApiError> handleCredentialsExpiredException(CredentialsExpiredException ex, HttpServletRequest request) {
        log.warn("Credentials expired: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(AuthErrorCode.CREDENTIALS_EXPIRED.getCode())
                .message(AuthErrorCode.CREDENTIALS_EXPIRED.getDefaultMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Catch-all for any other AuthenticationException from Spring Security.
     * Should come AFTER more specific handlers.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(AuthErrorCode.AUTHENTICATION_FAILED.getCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * User is authenticated but not authorized for this resource.
     * 
     * Returns: HTTP 403 Forbidden (not 401 Unauthorized)
     * 401 = "Who are you?" (not authenticated)
     * 403 = "I know who you are, but you can't do this" (not authorized)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .code(AuthErrorCode.ACCESS_DENIED.getCode())
                .message(AuthErrorCode.ACCESS_DENIED.getDefaultMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ========================= VALIDATION EXCEPTIONS =========================

    /**
     * Handle @Valid annotation validation failures.
     * 
     * Thrown when request body fails DTO validation (e.g., RegisterRequest).
     * Returns: HTTP 400 Bad Request with field-level error details.
     * 
     * This provides detailed feedback for each invalid field:
     * - field: Which field failed
     * - message: Why it failed
     * - rejectedValue: What value was submitted
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        // Extract field-level errors from the BindingResult
        BindingResult result = ex.getBindingResult();
        List<ApiError.FieldError> fieldErrors = result.getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()
                ))
                .toList();

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code("VALIDATION_ERROR")
                .message("Validation failed for one or more fields")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ========================= CATCH-ALL FOR UNEXPECTED ERRORS =========================

    /**
     * Handle any exception not caught by specific handlers.
     * 
     * Returns: HTTP 500 Internal Server Error
     * 
     * SECURITY: We log the full stack trace for debugging,
     * but return a generic message to the client (no internal details).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        // ERROR level (not WARN) because this is unexpected
        log.error("Unexpected error: ", ex);

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")  // Generic message - no details to client
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
