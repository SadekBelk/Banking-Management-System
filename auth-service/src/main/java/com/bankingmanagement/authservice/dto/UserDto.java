package com.bankingmanagement.authservice.dto;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                                  USER DTO                                                 ║
║                                                                                           ║
║  BUILD ORDER: STEP 4e of 12 (Fifth DTO)                                                  ║
║  PREVIOUS STEP: RefreshTokenRequest.java                                                 ║
║  NEXT STEP: ChangePasswordRequest.java (final DTO)                                       ║
║                                                                                           ║
║  WHAT THIS DTO DOES:                                                                     ║
║  - Returns user profile information (without sensitive data)                             ║
║  - Used by: GET /api/auth/me ("who am I?")                                              ║
║  - Safe to send to client (no password hash, no internal fields)                         ║
║                                                                                           ║
║  DTO vs ENTITY - WHAT'S EXPOSED:                                                         ║
║  ┌──────────────────────────────────────────────────────────────────┐                    ║
║  │  User Entity (DB)          │  UserDto (API Response)            │                    ║
║  │  ─────────────────────────────────────────────────────────────  │                    ║
║  │  id                        │  id                     ✓          │                    ║
║  │  username                  │  username               ✓          │                    ║
║  │  email                     │  email                  ✓          │                    ║
║  │  password (HASH!)          │  (NOT INCLUDED!)        ✗          │                    ║
║  │  firstName                 │  firstName              ✓          │                    ║
║  │  lastName                  │  lastName               ✓          │                    ║
║  │  phoneNumber               │  phoneNumber            ✓          │                    ║
║  │  enabled                   │  enabled                ✓          │                    ║
║  │  accountNonExpired         │  (NOT INCLUDED)         ✗          │                    ║
║  │  accountNonLocked          │  (NOT INCLUDED)         ✗          │                    ║
║  │  credentialsNonExpired     │  (NOT INCLUDED)         ✗          │                    ║
║  │  roles                     │  roles (as strings)     ✓          │                    ║
║  │  customerId                │  customerId             ✓          │                    ║
║  │  lastLoginAt               │  lastLoginAt            ✓          │                    ║
║  │  failedLoginAttempts       │  (NOT INCLUDED)         ✗          │                    ║
║  │  lockTime                  │  (NOT INCLUDED)         ✗          │                    ║
║  │  createdAt                 │  createdAt              ✓          │                    ║
║  │  updatedAt                 │  (NOT INCLUDED)         ✗          │                    ║
║  └──────────────────────────────────────────────────────────────────┘                    ║
║                                                                                           ║
║  WHY OMIT CERTAIN FIELDS?                                                                ║
║  - password: NEVER expose, even as hash (security)                                       ║
║  - accountNonLocked etc: Internal state, not useful to user                             ║
║  - failedLoginAttempts: Security info, shouldn't be exposed                             ║
║  - updatedAt: Not typically needed by clients                                           ║
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
 * DTO for user information responses.
 * 
 * This is a "read-only" DTO - used only for responses, not requests.
 * Provides user profile data without exposing sensitive information.
 * 
 * Returned by: GET /api/auth/me
 * 
 * EXAMPLE RESPONSE:
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "username": "john_doe",
 *   "email": "john@example.com",
 *   "firstName": "John",
 *   "lastName": "Doe",
 *   "phoneNumber": "+14155551234",
 *   "roles": ["ROLE_USER", "ROLE_CUSTOMER"],
 *   "enabled": true,
 *   "lastLoginAt": "2025-01-27T15:30:00Z",
 *   "createdAt": "2025-01-01T10:00:00Z"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Omit null fields from JSON
public class UserDto {

    /** User's unique identifier */
    private UUID id;

    /** User's chosen username */
    private String username;

    /** User's email address */
    private String email;

    /** User's first name (optional profile field) */
    private String firstName;

    /** User's last name (optional profile field) */
    private String lastName;

    /** User's phone number (optional profile field) */
    private String phoneNumber;

    /**
     * User's roles as string set.
     * Converted from Role entities: role.getName().name()
     * Example: ["ROLE_USER", "ROLE_CUSTOMER"]
     */
    private Set<String> roles;

    /** Whether the account is enabled (not disabled by admin) */
    private boolean enabled;

    /** Link to customer-service (for banking customers) */
    private UUID customerId;

    /** When the user last logged in (useful for security review) */
    private Instant lastLoginAt;

    /** When the account was created */
    private Instant createdAt;
}
