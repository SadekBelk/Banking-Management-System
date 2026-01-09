package com.bankingmanagement.authservice.dto;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                              AUTH RESPONSE DTO                                            ║
║                                                                                           ║
║  BUILD ORDER: STEP 4c of 12 (Third DTO - Most Important Response)                        ║
║  PREVIOUS STEP: LoginRequest.java                                                        ║
║  NEXT STEP: Other DTOs (RefreshTokenRequest, UserDto, ChangePasswordRequest)             ║
║                                                                                           ║
║  WHAT THIS DTO DOES:                                                                     ║
║  - Returns tokens and user info after successful login/registration                      ║
║  - Used by multiple endpoints: /login, /register, /refresh                               ║
║  - Contains everything the client needs to make authenticated requests                   ║
║                                                                                           ║
║  WHAT THE CLIENT DOES WITH THIS:                                                         ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  1. STORE THE TOKENS:                                                               │ ║
║  │     - accessToken: In memory (safer) or localStorage (convenient)                   │ ║
║  │     - refreshToken: In httpOnly cookie (safest) or secure storage                   │ ║
║  │                                                                                     │ ║
║  │  2. USE accessToken FOR API CALLS:                                                  │ ║
║  │     fetch('/api/accounts', {                                                        │ ║
║  │       headers: {                                                                    │ ║
║  │         'Authorization': `${tokenType} ${accessToken}`                             │ ║
║  │         // Results in: 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR...'          │ ║
║  │       }                                                                             │ ║
║  │     })                                                                              │ ║
║  │                                                                                     │ ║
║  │  3. REFRESH BEFORE EXPIRY:                                                          │ ║
║  │     - Check expiresAt or expiresIn before each request                             │ ║
║  │     - If expired/expiring: Call /api/auth/refresh with refreshToken                │ ║
║  │     - Get new accessToken                                                          │ ║
║  │                                                                                     │ ║
║  │  4. USE USER INFO FOR UI:                                                           │ ║
║  │     - Display username/email in navigation                                          │ ║
║  │     - Check roles for showing/hiding features                                       │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  EXAMPLE RESPONSE:                                                                        ║
║  {                                                                                        ║
║    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",                           ║
║    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",                              ║
║    "tokenType": "Bearer",                                                                ║
║    "expiresIn": 86400,                                                                   ║
║    "expiresAt": "2025-01-28T10:30:00Z",                                                 ║
║    "userId": "550e8400-e29b-41d4-a716-446655440000",                                    ║
║    "username": "john_doe",                                                               ║
║    "email": "john@example.com",                                                          ║
║    "roles": ["ROLE_USER", "ROLE_CUSTOMER"]                                              ║
║  }                                                                                        ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for authentication responses containing JWT tokens and user information.
 * 
 * Returned by:
 * - POST /api/auth/register (after successful registration)
 * - POST /api/auth/login (after successful login)
 * - POST /api/auth/refresh (after successful token refresh)
 * 
 * @JsonInclude(NON_NULL) ensures null fields are omitted from JSON,
 * resulting in cleaner responses (e.g., /refresh doesn't return user info).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Don't include null fields in JSON
public class AuthResponse {

    /**
     * JWT access token for authenticating API requests.
     * 
     * - This is a JWT (JSON Web Token)
     * - Contains: userId, username, email, roles (as claims)
     * - Signed with server's secret key
     * - Client sends this in Authorization header
     * - Short-lived (24 hours in our config)
     */
    private String accessToken;

    /**
     * Refresh token for obtaining new access tokens.
     * 
     * - This is a random UUID string (NOT a JWT)
     * - Stored in database (allows revocation)
     * - Long-lived (7 days in our config)
     * - Only sent to /api/auth/refresh endpoint
     * - Should be stored securely on client
     */
    private String refreshToken;

    /**
     * Token type for the Authorization header.
     * 
     * Always "Bearer" for JWT tokens.
     * Client uses: `Authorization: Bearer <accessToken>`
     * 
     * @Builder.Default ensures this is set even when using builder pattern.
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Seconds until the access token expires.
     * 
     * Useful for:
     * - Setting timers to refresh token before expiry
     * - Showing "session expires in X minutes" to user
     * 
     * Example: 86400 = 24 hours
     */
    private Long expiresIn;

    /**
     * Exact timestamp when the access token expires.
     * 
     * Alternative to expiresIn - some clients prefer absolute time.
     * ISO 8601 format: "2025-01-28T10:30:00Z"
     */
    private Instant expiresAt;

    // ========================= USER INFORMATION =========================
    /*
     * Included so client doesn't need to decode the JWT to get user info.
     * Makes it easier to display user details immediately after login.
     */

    /** User's unique identifier */
    private UUID userId;

    /** User's chosen username */
    private String username;

    /** User's email address */
    private String email;

    /**
     * User's assigned roles.
     * 
     * Example: ["ROLE_USER", "ROLE_CUSTOMER"]
     * 
     * Client can use this for:
     * - Showing/hiding UI elements based on role
     * - Client-side route protection
     * - Feature toggles
     */
    private Set<String> roles;

    /**
     * Optional message for additional context.
     * 
     * Examples:
     * - "Login successful"
     * - "Registration complete. Welcome!"
     * - "Token refreshed successfully"
     */
    private String message;
}
