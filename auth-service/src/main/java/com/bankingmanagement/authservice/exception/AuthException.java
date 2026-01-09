package com.bankingmanagement.authservice.exception;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                              AUTH EXCEPTION (BASE CLASS)                                  ║
║                                                                                           ║
║  BUILD ORDER: STEP 6b of 12 (Second Exception Component)                                 ║
║  PREVIOUS STEP: AuthErrorCode enum                                                       ║
║  NEXT STEP: Specific exception classes (InvalidTokenException, ResourceNotFoundException)║
║                                                                                           ║
║  WHY A BASE EXCEPTION CLASS?                                                             ║
║  - Common fields (errorCode) shared by all auth exceptions                               ║
║  - Allows catching all auth errors: catch (AuthException e)                              ║
║  - GlobalExceptionHandler can handle all auth errors uniformly                           ║
║  - Subclasses provide more specific error types                                          ║
║                                                                                           ║
║  EXCEPTION HIERARCHY:                                                                    ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  RuntimeException                                                                   │ ║
║  │      │                                                                              │ ║
║  │      └── AuthException (this class)                                                 │ ║
║  │              │                                                                      │ ║
║  │              └── InvalidTokenException                                              │ ║
║  │                                                                                     │ ║
║  │  RuntimeException                                                                   │ ║
║  │      │                                                                              │ ║
║  │      └── ResourceNotFoundException (separate hierarchy)                             │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY RuntimeException (UNCHECKED) NOT Exception (CHECKED)?                               ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  CHECKED EXCEPTION (extends Exception):                                             │ ║
║  │  - Must declare in method signature: throws AuthException                           │ ║
║  │  - Must catch or re-throw at every level                                            │ ║
║  │  - Clutters code with try-catch everywhere                                          │ ║
║  │                                                                                     │ ║
║  │  UNCHECKED EXCEPTION (extends RuntimeException) - OUR CHOICE:                       │ ║
║  │  - Propagates automatically up the call stack                                       │ ║
║  │  - Caught globally by @RestControllerAdvice                                         │ ║
║  │  - Clean service code without try-catch noise                                       │ ║
║  │  - Modern best practice for business exceptions                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base exception for authentication-related errors.
 * 
 * Extends RuntimeException (unchecked) so it propagates automatically
 * to the GlobalExceptionHandler without explicit throws declarations.
 * 
 * @ResponseStatus(UNAUTHORIZED) sets default HTTP status to 401 if
 * this exception reaches Spring's default error handling.
 * (Our GlobalExceptionHandler overrides this with more control.)
 * 
 * USAGE:
 * throw new AuthException("Invalid credentials", AuthErrorCode.INVALID_CREDENTIALS);
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)  // Default HTTP 401
public class AuthException extends RuntimeException {

    /** Error code for programmatic handling */
    private final AuthErrorCode errorCode;

    /**
     * Constructor with just a message.
     * Uses default error code AUTHENTICATION_FAILED.
     */
    public AuthException(String message) {
        super(message);
        this.errorCode = AuthErrorCode.AUTHENTICATION_FAILED;
    }

    /**
     * Constructor with message and specific error code.
     * PREFERRED: Always use specific error codes when possible.
     * 
     * Example:
     * throw new AuthException("User account is locked", AuthErrorCode.ACCOUNT_LOCKED);
     */
    public AuthException(String message, AuthErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor with message and cause (wrapped exception).
     * Use when catching a lower-level exception.
     * 
     * Example:
     * catch (JDBCException e) {
     *     throw new AuthException("Database error during login", e);
     * }
     */
    public AuthException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = AuthErrorCode.AUTHENTICATION_FAILED;
    }

    /**
     * Full constructor with message, error code, and cause.
     * Most detailed option for error reporting.
     */
    public AuthException(String message, AuthErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Get the error code associated with this exception.
     * Used by GlobalExceptionHandler to include in API response.
     */
    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
