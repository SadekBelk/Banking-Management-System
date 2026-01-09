package com.bankingmanagement.authservice.exception;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                       RESOURCE NOT FOUND EXCEPTION                                        ║
║                                                                                           ║
║  BUILD ORDER: STEP 6d of 12 (Fourth Exception Component)                                 ║
║  PREVIOUS STEP: InvalidTokenException                                                    ║
║  NEXT STEP: ApiError (response format)                                                   ║
║                                                                                           ║
║  WHAT THIS EXCEPTION DOES:                                                               ║
║  - Thrown when requested entity doesn't exist                                            ║
║  - Results in HTTP 404 Not Found response                                                ║
║  - Different HTTP status than AuthException (401 vs 404)                                 ║
║                                                                                           ║
║  WHY NOT EXTEND AuthException?                                                           ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  AuthException                       ResourceNotFoundException                      │ ║
║  │  - HTTP 401 Unauthorized              - HTTP 404 Not Found                           │ ║
║  │  - Authentication failure             - Resource doesn't exist                       │ ║
║  │  - User can retry with correct creds  - Nothing to retry, resource missing          │ ║
║  │                                                                                     │ ║
║  │  Different semantics = different exception hierarchy                                │ ║
║  │  But both use AuthErrorCode for consistency                                         │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHEN THIS IS THROWN:                                                                    ║
║  - GET /api/auth/me with valid JWT but user was deleted                                 ║
║  - Registration referencing non-existent role                                           ║
║  - Any lookup that should find an entity but doesn't                                    ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource is not found.
 * 
 * Separate from AuthException because:
 * - Different HTTP status (404 vs 401)
 * - Different meaning (missing resource vs auth failure)
 * - Different client handling
 * 
 * USAGE EXAMPLES:
 * 
 * // User not found
 * throw new ResourceNotFoundException("User not found with id: " + userId);
 * 
 * // Role not found
 * throw new ResourceNotFoundException("Role not found: ROLE_ADMIN", AuthErrorCode.ROLE_NOT_FOUND);
 */
@ResponseStatus(HttpStatus.NOT_FOUND)  // HTTP 404
public class ResourceNotFoundException extends RuntimeException {

    /** Error code for programmatic handling (reuses AuthErrorCode for consistency) */
    private final AuthErrorCode errorCode;

    /**
     * Constructor with message only.
     * Uses default error code USER_NOT_FOUND.
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = AuthErrorCode.USER_NOT_FOUND;
    }

    /**
     * Constructor with message and specific error code.
     * Use for non-user resources:
     * - ROLE_NOT_FOUND
     */
    public ResourceNotFoundException(String message, AuthErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Get the error code for inclusion in API response.
     */
    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
