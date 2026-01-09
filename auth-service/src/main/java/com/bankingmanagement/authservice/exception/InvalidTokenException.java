package com.bankingmanagement.authservice.exception;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                         INVALID TOKEN EXCEPTION                                           ║
║                                                                                           ║
║  BUILD ORDER: STEP 6c of 12 (Third Exception Component)                                  ║
║  PREVIOUS STEP: AuthException base class                                                 ║
║  NEXT STEP: ResourceNotFoundException                                                    ║
║                                                                                           ║
║  WHAT THIS EXCEPTION DOES:                                                               ║
║  - Thrown when JWT or Refresh Token validation fails                                     ║
║  - More specific than generic AuthException                                              ║
║  - Allows special handling of token-related errors                                       ║
║                                                                                           ║
║  WHEN THIS IS THROWN:                                                                    ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  JwtTokenProvider.validateToken():                                                   │ ║
║  │  - JWT signature doesn't match (tampered or wrong secret)                           │ ║
║  │  - JWT is malformed (invalid JSON structure)                                        │ ║
║  │  - JWT is expired                                                                   │ ║
║  │                                                                                     │ ║
║  │  AuthService.refreshToken():                                                        │ ║
║  │  - Refresh token not found in database                                              │ ║
║  │  - Refresh token is revoked                                                         │ ║
║  │  - Refresh token is expired                                                         │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY EXTEND AuthException (not RuntimeException directly)?                               ║
║  - Inherits errorCode field and methods                                                  ║
║  - Can be caught with: catch (AuthException e) (catches all auth errors)                ║
║  - OR specifically: catch (InvalidTokenException e) (catches only token errors)         ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an invalid token is provided.
 * 
 * This is a specialized AuthException for token-specific errors.
 * Provides clearer error messages for token issues.
 * 
 * USAGE EXAMPLES:
 * 
 * // JWT validation failed
 * throw new InvalidTokenException("JWT signature verification failed");
 * 
 * // Refresh token expired
 * throw new InvalidTokenException("Refresh token has expired", AuthErrorCode.TOKEN_EXPIRED);
 * 
 * // Refresh token revoked
 * throw new InvalidTokenException("Token has been revoked", AuthErrorCode.TOKEN_REVOKED);
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)  // HTTP 401
public class InvalidTokenException extends AuthException {

    /**
     * Constructor with message only.
     * Uses default error code INVALID_TOKEN.
     */
    public InvalidTokenException(String message) {
        super(message, AuthErrorCode.INVALID_TOKEN);
    }

    /**
     * Constructor with message and specific error code.
     * Use for more specific token errors:
     * - TOKEN_EXPIRED
     * - TOKEN_REVOKED
     * - REFRESH_TOKEN_NOT_FOUND
     * - INVALID_REFRESH_TOKEN
     */
    public InvalidTokenException(String message, AuthErrorCode errorCode) {
        super(message, errorCode);
    }
}
