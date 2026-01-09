package com.bankingmanagement.authservice.dto;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                           REFRESH TOKEN REQUEST DTO                                       ║
║                                                                                           ║
║  BUILD ORDER: STEP 4d of 12 (Fourth DTO)                                                 ║
║  PREVIOUS STEP: AuthResponse.java                                                        ║
║  NEXT STEP: UserDto.java                                                                 ║
║                                                                                           ║
║  WHAT THIS DTO DOES:                                                                     ║
║  - Carries the refresh token from client to server                                       ║
║  - Used when access token expires and client needs a new one                             ║
║                                                                                           ║
║  THE TOKEN REFRESH FLOW:                                                                 ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  CLIENT                                SERVER                                       │ ║
║  │                                                                                     │ ║
║  │  1. Access token expires (after 24h)                                               │ ║
║  │     ↓                                                                              │ ║
║  │  2. POST /api/auth/refresh             →                                           │ ║
║  │     { "refreshToken": "abc-123..." }                                              │ ║
║  │                                        3. Server looks up token in DB              │ ║
║  │                                        4. Validates: not expired, not revoked      │ ║
║  │                                        5. Generates new access token               │ ║
║  │                              ←         6. Returns AuthResponse with new token      │ ║
║  │  7. Store new access token                                                         │ ║
║  │  8. Continue making API calls                                                      │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY SO SIMPLE?                                                                          ║
║  - Only the refresh token is needed                                                      ║
║  - No username/password required (that's the point of refresh tokens!)                   ║
║  - Server already knows who the user is from the DB                                      ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for refresh token requests.
 * 
 * Used when the client's access token has expired and they need a new one
 * without re-entering their username and password.
 * 
 * EXAMPLE REQUEST:
 * POST /api/auth/refresh
 * Content-Type: application/json
 * 
 * {
 *   "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /**
     * The refresh token issued during login.
     * 
     * This is the random UUID string (NOT a JWT) that was stored in the
     * database when the user logged in. Server will look it up in the
     * refresh_tokens table to validate it.
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
