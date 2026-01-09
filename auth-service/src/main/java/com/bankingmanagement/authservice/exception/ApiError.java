package com.bankingmanagement.authservice.exception;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                              API ERROR RESPONSE FORMAT                                    ║
║                                                                                           ║
║  BUILD ORDER: STEP 6e of 12 (Fifth Exception Component)                                  ║
║  PREVIOUS STEP: ResourceNotFoundException                                                 ║
║  NEXT STEP: GlobalExceptionHandler                                                       ║
║                                                                                           ║
║  WHAT THIS CLASS DOES:                                                                   ║
║  - Defines the STANDARD format for ALL error responses                                   ║
║  - Used by GlobalExceptionHandler to serialize errors                                    ║
║  - Provides consistent error structure across entire API                                 ║
║                                                                                           ║
║  WHY A STANDARD ERROR FORMAT?                                                            ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  WITHOUT STANDARD FORMAT (BAD):                                                     │ ║
║  │  // Sometimes returns:                                                              │ ║
║  │  { "error": "Bad Request" }                                                         │ ║
║  │  // Sometimes returns:                                                              │ ║
║  │  { "message": "Invalid input" }                                                     │ ║
║  │  // Sometimes returns:                                                              │ ║
║  │  { "errors": ["Field required"] }                                                   │ ║
║  │                                                                                     │ ║
║  │  WITH STANDARD FORMAT (GOOD - OUR APPROACH):                                        │ ║
║  │  {                                                                                  │ ║
║  │    "timestamp": "2025-01-27T10:30:00Z",                                            │ ║
║  │    "status": 400,                                                                   │ ║
║  │    "error": "Bad Request",                                                          │ ║
║  │    "code": "AUTH020",                                                               │ ║
║  │    "message": "Username already exists",                                            │ ║
║  │    "path": "/api/auth/register",                                                    │ ║
║  │    "fieldErrors": [...]                                                             │ ║
║  │  }                                                                                  │ ║
║  │                                                                                     │ ║
║  │  BENEFITS:                                                                          │ ║
║  │  - Clients always know what fields to expect                                        │ ║
║  │  - Error handling code is simpler                                                   │ ║
║  │  - Logging and monitoring is consistent                                             │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY JAVA RECORD (not class)?                                                            ║
║  - Records are immutable (error responses shouldn't change)                              ║
║  - Automatically generates: constructor, getters, equals, hashCode, toString             ║
║  - Concise syntax: 10 lines instead of 50+                                              ║
║  - Perfect for DTOs/value objects like error responses                                   ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error response format.
 * 
 * This is what clients receive when any error occurs.
 * Consistent structure makes client error handling simple.
 * 
 * @param timestamp When the error occurred
 * @param status    HTTP status code (400, 401, 404, 500, etc.)
 * @param error     HTTP status phrase ("Bad Request", "Unauthorized", etc.)
 * @param code      Application error code ("AUTH002") for programmatic handling
 * @param message   Human-readable error description
 * @param path      Request URI that caused the error
 * @param fieldErrors List of field-level validation errors (for 400 Bad Request)
 * 
 * EXAMPLE RESPONSE (login failure):
 * {
 *   "timestamp": "2025-01-27T10:30:00Z",
 *   "status": 401,
 *   "error": "Unauthorized",
 *   "code": "AUTH002",
 *   "message": "Invalid username/email or password",
 *   "path": "/api/auth/login"
 * }
 * 
 * EXAMPLE RESPONSE (validation failure):
 * {
 *   "timestamp": "2025-01-27T10:30:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/auth/register",
 *   "fieldErrors": [
 *     { "field": "email", "message": "Invalid email format", "rejectedValue": "not-an-email" },
 *     { "field": "password", "message": "Password must be at least 8 characters", "rejectedValue": "abc" }
 *   ]
 * }
 */
@Builder  // Lombok: Enables builder pattern for constructing responses
@JsonInclude(JsonInclude.Include.NON_NULL)  // Don't include null fields in JSON
public record ApiError(
        /** When the error occurred (ISO 8601 format in JSON) */
        Instant timestamp,

        /** HTTP status code (e.g., 400, 401, 404, 500) */
        int status,

        /** HTTP status phrase (e.g., "Bad Request", "Unauthorized") */
        String error,

        /** Application-specific error code (e.g., "AUTH002") */
        String code,

        /** Human-readable error message */
        String message,

        /** Request URI that caused the error */
        String path,

        /** List of field-level validation errors (null if not a validation error) */
        List<FieldError> fieldErrors
) {
    /**
     * Nested record for field-level validation errors.
     * 
     * Used when @Valid fails on request body.
     * Provides specific feedback for each invalid field.
     * 
     * @param field         Name of the field that failed validation
     * @param message       Validation error message
     * @param rejectedValue The value that was rejected (for debugging)
     */
    public record FieldError(
            String field,
            String message,
            Object rejectedValue
    ) {}
}
