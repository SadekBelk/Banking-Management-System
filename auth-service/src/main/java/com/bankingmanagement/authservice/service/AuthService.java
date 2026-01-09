package com.bankingmanagement.authservice.service;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                            AUTH SERVICE INTERFACE                                         ║
║                                                                                           ║
║  BUILD ORDER: STEP 8a of 12 (Service Layer - Interface First)                            ║
║  PREVIOUS STEP: Security Components (JwtAccessDeniedHandler was last)                    ║
║  NEXT STEP: AuthServiceImpl (implements this interface)                                  ║
║                                                                                           ║
║  WHY INTERFACE + IMPLEMENTATION PATTERN?                                                 ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  INTERFACE-FIRST DESIGN:                                                                 ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  AuthService (interface)                                                            │ ║
║  │       │                                                                             │ ║
║  │       ├── AuthServiceImpl (production implementation)                               │ ║
║  │       │                                                                             │ ║
║  │       └── MockAuthService (test implementation - if needed)                         │ ║
║  │                                                                                     │ ║
║  │  BENEFITS:                                                                          │ ║
║  │  1. Loose coupling - Controller depends on interface, not concrete class            │ ║
║  │  2. Testability - Easy to mock in unit tests                                        │ ║
║  │  3. Flexibility - Can swap implementations (e.g., OAuth2 impl later)                │ ║
║  │  4. Clean contracts - Interface defines WHAT, implementation defines HOW            │ ║
║  │  5. Spring DI - @Autowired works with interfaces (proxy-based AOP)                  │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  SERVICE LAYER RESPONSIBILITIES:                                                         ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  Controller                     Service                     Repository             │ ║
║  │  ┌──────────────┐              ┌──────────────┐            ┌──────────────┐        │ ║
║  │  │ HTTP handling│  ────────>   │ Business     │  ────────> │ Data access  │        │ ║
║  │  │ Validation   │              │ logic        │            │ JPA/SQL      │        │ ║
║  │  │ Auth check   │  <────────   │ Transactions │  <──────── │              │        │ ║
║  │  └──────────────┘              └──────────────┘            └──────────────┘        │ ║
║  │                                                                                     │ ║
║  │  Service Layer Does:                                                                │ ║
║  │  - Business logic (password hashing, token generation)                              │ ║
║  │  - Transaction management (@Transactional)                                          │ ║
║  │  - Multiple repository coordination                                                 │ ║
║  │  - Domain exception throwing                                                        │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  AUTH SERVICE OPERATIONS:                                                                ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  AUTHENTICATION OPERATIONS:                                                         │ ║
║  │  • register()  - New user signup with password hashing                              │ ║
║  │  • login()     - Validate credentials, return JWT tokens                            │ ║
║  │  • validateToken() - Check if access token is valid                                 │ ║
║  │                                                                                     │ ║
║  │  TOKEN MANAGEMENT:                                                                  │ ║
║  │  • refreshToken() - Exchange refresh token for new access token                     │ ║
║  │  • logout()       - Revoke single refresh token                                     │ ║
║  │  • logoutAll()    - Revoke all refresh tokens (all devices)                         │ ║
║  │                                                                                     │ ║
║  │  USER OPERATIONS:                                                                   │ ║
║  │  • getCurrentUser()   - Get authenticated user's profile                            │ ║
║  │  • changePassword()   - Change password with validation                             │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.dto.*;
import com.bankingmanagement.authservice.exception.AuthException;
import com.bankingmanagement.authservice.exception.InvalidTokenException;
import com.bankingmanagement.authservice.exception.ResourceNotFoundException;
import com.bankingmanagement.authservice.security.UserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Service interface for authentication operations.
 * 
 * Defines the contract for authentication-related business logic.
 * Implementation handles password encoding, token generation, and user management.
 * 
 * USAGE: Controllers inject this interface (Spring resolves to AuthServiceImpl)
 * 
 * @see com.bankingmanagement.authservice.service.impl.AuthServiceImpl
 */
public interface AuthService {

    // ==================== AUTHENTICATION OPERATIONS ====================

    /**
     * Register a new user.
     * 
     * Process:
     * 1. Validate username/email uniqueness
     * 2. Hash password (bcrypt)
     * 3. Assign default role (ROLE_USER)
     * 4. Create user in database
     * 5. Generate access + refresh tokens
     * 
     * @param request Registration details (username, email, password, etc.)
     * @return AuthResponse with tokens and user info
     * @throws AuthException if username/email already exists
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate user and return tokens.
     * 
     * Process:
     * 1. Validate credentials via AuthenticationManager
     * 2. Update last login timestamp
     * 3. Generate JWT access token
     * 4. Generate and store refresh token
     * 
     * @param request Login credentials (username/email + password)
     * @return AuthResponse with tokens and user info
     * @throws BadCredentialsException if credentials invalid
     */
    AuthResponse login(LoginRequest request);

    /**
     * Validate access token.
     * 
     * Checks if token is:
     * - Properly signed (not tampered)
     * - Not expired
     * - Well-formed JWT
     * 
     * Used by other services to validate tokens they receive.
     * 
     * @param token JWT access token string
     * @return true if valid, false otherwise
     */
    boolean validateToken(String token);

    // ==================== TOKEN MANAGEMENT ====================

    /**
     * Refresh access token using refresh token.
     * 
     * Process (Token Rotation):
     * 1. Find refresh token in database
     * 2. Validate not revoked/expired
     * 3. Revoke old refresh token
     * 4. Generate new access token
     * 5. Generate new refresh token
     * 6. Link old → new for audit trail
     * 
     * WHY TOKEN ROTATION?
     * - Limits damage from stolen refresh tokens
     * - Each refresh token can only be used once
     * - Reuse attempt reveals token theft
     * 
     * @param request Contains refresh token string
     * @return AuthResponse with new tokens
     * @throws InvalidTokenException if refresh token invalid/revoked/expired
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Logout user and revoke refresh token.
     * 
     * Revokes the specific refresh token (single device logout).
     * Access token remains valid until expiry (stateless limitation).
     * 
     * @param refreshToken The refresh token to revoke
     */
    void logout(String refreshToken);

    /**
     * Logout from all devices (revoke all refresh tokens).
     * 
     * Revokes ALL refresh tokens for the user.
     * Use when:
     * - User suspects account compromise
     * - Password changed (force re-login everywhere)
     * - Account deactivation
     * 
     * @param userPrincipal Current authenticated user
     */
    void logoutAll(UserPrincipal userPrincipal);

    // ==================== USER OPERATIONS ====================

    /**
     * Get current user information.
     * 
     * Returns full user profile for authenticated user.
     * Does NOT return password (security).
     * 
     * @param userPrincipal Current authenticated user
     * @return UserDto with user profile data
     * @throws ResourceNotFoundException if user no longer exists
     */
    UserDto getCurrentUser(UserPrincipal userPrincipal);

    /**
     * Change user password.
     * 
     * Process:
     * 1. Validate current password is correct
     * 2. Validate new password != current password
     * 3. Validate new password matches confirmation
     * 4. Hash and store new password
     * 5. Revoke all refresh tokens (force re-login)
     * 
     * @param userPrincipal Current authenticated user
     * @param request Contains current password, new password, confirmation
     * @throws AuthException if validation fails
     */
    void changePassword(UserPrincipal userPrincipal, ChangePasswordRequest request);
}
