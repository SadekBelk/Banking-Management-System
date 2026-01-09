package com.bankingmanagement.authservice.service.impl;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                          AUTH SERVICE IMPLEMENTATION                                      ║
║                                                                                           ║
║  BUILD ORDER: STEP 8b of 12 (Service Layer - Implementation)                             ║
║  PREVIOUS STEP: AuthService interface (defines the contract we implement)                ║
║  NEXT STEP: Configuration classes (SecurityConfig, DataInitializer)                      ║
║                                                                                           ║
║  THIS IS THE CORE BUSINESS LOGIC FOR AUTHENTICATION                                      ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  DEPENDENCIES INJECTED:                                                                  ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  REPOSITORIES (Data Access):                                                        │ ║
║  │  • UserRepository       - CRUD for User entities                                    │ ║
║  │  • RoleRepository       - Find roles by name                                        │ ║
║  │  • RefreshTokenRepository - Manage refresh tokens                                   │ ║
║  │                                                                                     │ ║
║  │  SECURITY COMPONENTS:                                                               │ ║
║  │  • PasswordEncoder      - BCrypt hashing                                            │ ║
║  │  • AuthenticationManager - Validates credentials                                    │ ║
║  │  • JwtTokenProvider     - Generate/validate JWTs                                    │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  TRANSACTION MANAGEMENT:                                                                 ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  @Transactional - Methods run within database transaction                           │ ║
║  │                                                                                     │ ║
║  │  DEFAULT (without readOnly):                                                        │ ║
║  │  - Transaction starts when method begins                                            │ ║
║  │  - Commits if method completes normally                                             │ ║
║  │  - Rolls back if RuntimeException thrown                                            │ ║
║  │                                                                                     │ ║
║  │  @Transactional(readOnly = true):                                                   │ ║
║  │  - Read-only optimization (no flush, potential caching)                             │ ║
║  │  - Used for queries that don't modify data                                          │ ║
║  │                                                                                     │ ║
║  │  EXAMPLE: register() creates User AND RefreshToken in ONE transaction               │ ║
║  │           If RefreshToken save fails, User creation rolls back too                  │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  AUTHENTICATION FLOW DETAIL:                                                             ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  LOGIN PROCESS:                                                                     │ ║
║  │  ┌──────────────────────────────────────────────────────────────────────────────┐  │ ║
║  │  │ 1. AuthenticationManager.authenticate()                                      │  │ ║
║  │  │    ├── Calls CustomUserDetailsService.loadUserByUsername()                   │  │ ║
║  │  │    ├── Creates UserPrincipal from User entity                                │  │ ║
║  │  │    ├── Compares provided password with stored hash                           │  │ ║
║  │  │    └── Returns Authentication with UserPrincipal                             │  │ ║
║  │  │                                                                              │  │ ║
║  │  │ 2. Extract UserPrincipal from Authentication                                 │  │ ║
║  │  │                                                                              │  │ ║
║  │  │ 3. Update lastLoginAt timestamp in database                                  │  │ ║
║  │  │                                                                              │  │ ║
║  │  │ 4. Generate JWT access token with claims:                                    │  │ ║
║  │  │    { sub: userId, username, email, roles, iat, exp }                         │  │ ║
║  │  │                                                                              │  │ ║
║  │  │ 5. Generate refresh token (random UUID) and store in DB                      │  │ ║
║  │  │                                                                              │  │ ║
║  │  │ 6. Return AuthResponse with both tokens                                      │  │ ║
║  │  └──────────────────────────────────────────────────────────────────────────────┘  │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  REFRESH TOKEN ROTATION (Security Best Practice):                                        ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  TRADITIONAL APPROACH (reusable refresh tokens):                                    │ ║
║  │  - Same refresh token used multiple times                                           │ ║
║  │  - If stolen, attacker has long-term access                                         │ ║
║  │                                                                                     │ ║
║  │  OUR APPROACH (token rotation):                                                     │ ║
║  │  ┌────────────────────────────────────────────────────────────────────────────┐    │ ║
║  │  │ refresh_token_v1 ───────> refresh_token_v2 ───────> refresh_token_v3      │    │ ║
║  │  │     (revoked)                 (revoked)                 (active)          │    │ ║
║  │  │                                                                            │    │ ║
║  │  │ Each refresh returns NEW refresh token, OLD one is invalidated             │    │ ║
║  │  │ If attacker tries to use revoked token, we know theft occurred!            │    │ ║
║  │  │                                                                            │    │ ║
║  │  │ replacedByToken field: Creates audit trail                                 │    │ ║
║  │  │ v1.replacedByToken = v2.token                                              │    │ ║
║  │  │ v2.replacedByToken = v3.token                                              │    │ ║
║  │  └────────────────────────────────────────────────────────────────────────────┘    │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.dto.*;
import com.bankingmanagement.authservice.entity.RefreshToken;
import com.bankingmanagement.authservice.entity.Role;
import com.bankingmanagement.authservice.entity.User;
import com.bankingmanagement.authservice.exception.AuthErrorCode;
import com.bankingmanagement.authservice.exception.AuthException;
import com.bankingmanagement.authservice.exception.InvalidTokenException;
import com.bankingmanagement.authservice.exception.ResourceNotFoundException;
import com.bankingmanagement.authservice.repository.RefreshTokenRepository;
import com.bankingmanagement.authservice.repository.RoleRepository;
import com.bankingmanagement.authservice.repository.UserRepository;
import com.bankingmanagement.authservice.security.JwtTokenProvider;
import com.bankingmanagement.authservice.security.UserPrincipal;
import com.bankingmanagement.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of AuthService.
 * Handles user registration, authentication, and token management.
 * 
 * This is the CORE business logic layer:
 * - Coordinates between repositories, security components
 * - Manages transactions
 * - Implements authentication workflows
 * 
 * @Service marks this as Spring service bean (auto-detected via component scan)
 * @RequiredArgsConstructor generates constructor with all final fields (dependency injection)
 * @Slf4j generates logger: log.info(), log.debug(), log.error()
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    /*
     * REPOSITORY DEPENDENCIES - Data access layer
     * All injected via constructor (RequiredArgsConstructor)
     */
    private final UserRepository userRepository;           // User CRUD
    private final RoleRepository roleRepository;           // Role lookup
    private final RefreshTokenRepository refreshTokenRepository; // Refresh token management

    /*
     * SECURITY DEPENDENCIES
     */
    private final PasswordEncoder passwordEncoder;         // BCrypt hashing
    private final AuthenticationManager authenticationManager; // Credential validation
    private final JwtTokenProvider jwtTokenProvider;       // JWT generation/validation

    // ==================== REGISTRATION ====================

    /**
     * Register a new user in the system.
     * 
     * Complete registration workflow:
     * 1. Uniqueness validation (username, email)
     * 2. Password hashing
     * 3. Role assignment
     * 4. User persistence
     * 5. Token generation
     */
    @Override
    @Transactional  // Ensures all DB operations succeed or all roll back
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        // VALIDATION 1: Check if username already taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException(
                    "Username '" + request.getUsername() + "' is already taken",
                    AuthErrorCode.USERNAME_ALREADY_EXISTS
            );
        }

        // VALIDATION 2: Check if email already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException(
                    "Email '" + request.getEmail() + "' is already registered",
                    AuthErrorCode.EMAIL_ALREADY_EXISTS
            );
        }

        // Get default role (ROLE_USER) - must exist in database
        // DataInitializer creates this role on startup
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Default role not found",
                        AuthErrorCode.ROLE_NOT_FOUND
                ));

        /*
         * CREATE USER ENTITY
         * 
         * passwordEncoder.encode() - BCrypt hashing:
         * - Input: "password123"
         * - Output: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
         * 
         * Account status flags all true for new accounts
         */
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // NEVER store plaintext!
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)               // Account is active
                .accountNonExpired(true)     // Account not expired
                .accountNonLocked(true)      // Account not locked
                .credentialsNonExpired(true) // Password not expired
                .build();

        // Assign default role (many-to-many relationship)
        user.addRole(userRole);
        
        // Persist to database (JPA assigns UUID primary key)
        user = userRepository.save(user);

        log.info("User registered successfully: {}", user.getUsername());

        // GENERATE TOKENS for immediate login after registration
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        RefreshToken refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken.getToken(), "Registration successful");
    }

    // ==================== LOGIN ====================

    /**
     * Authenticate user with credentials and return tokens.
     * 
     * Uses Spring Security's AuthenticationManager which:
     * 1. Calls CustomUserDetailsService.loadUserByUsername()
     * 2. Compares passwords using PasswordEncoder.matches()
     * 3. Throws BadCredentialsException if invalid
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        /*
         * AUTHENTICATE via Spring Security
         * 
         * UsernamePasswordAuthenticationToken:
         * - First param (principal): username/email to look up
         * - Second param (credentials): plaintext password to verify
         * 
         * AuthenticationManager.authenticate():
         * - Throws BadCredentialsException if invalid (caught by GlobalExceptionHandler)
         * - Returns Authentication with UserPrincipal if valid
         */
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        // Extract UserPrincipal from successful authentication
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Update last login timestamp (for audit/display)
        userRepository.updateLoginSuccess(userPrincipal.getId(), Instant.now());

        // Generate JWT access token
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        
        // Load full user for response (UserPrincipal doesn't have all fields)
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate and store refresh token in database
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User logged in successfully: {}", userPrincipal.getUsername());

        return buildAuthResponse(user, accessToken, refreshToken.getToken(), "Login successful");
    }

    // ==================== TOKEN REFRESH ====================

    /**
     * Exchange refresh token for new tokens (Token Rotation).
     * 
     * Security measures:
     * 1. Validate refresh token exists and is valid
     * 2. Revoke old refresh token (single use)
     * 3. Create audit trail (replacedByToken)
     */
    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");

        // STEP 1: Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException(
                        "Refresh token not found",
                        AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
                ));

        // STEP 2: Validate refresh token is still usable
        if (!refreshToken.isValid()) {
            if (refreshToken.isRevoked()) {
                // Token already used - possible replay attack or reuse attempt
                throw new InvalidTokenException("Refresh token has been revoked", AuthErrorCode.TOKEN_REVOKED);
            }
            if (refreshToken.isExpired()) {
                // Token expired - user must login again
                throw new InvalidTokenException("Refresh token has expired", AuthErrorCode.TOKEN_EXPIRED);
            }
        }

        User user = refreshToken.getUser();

        // STEP 3: REVOKE old refresh token (token rotation)
        // This prevents token reuse - each refresh token works only ONCE
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        // STEP 4: Generate new tokens
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        RefreshToken newRefreshToken = createRefreshToken(user);

        // STEP 5: Create audit trail (old token → new token)
        // Useful for security investigation if token theft suspected
        refreshToken.setReplacedByToken(newRefreshToken.getToken());
        refreshTokenRepository.save(refreshToken);

        log.info("Token refreshed successfully for user: {}", user.getUsername());

        return buildAuthResponse(user, newAccessToken, newRefreshToken.getToken(), "Token refreshed successfully");
    }

    // ==================== LOGOUT ====================

    /**
     * Logout from single device (revoke specific refresh token).
     */
    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.info("Logging out user");

        // Find and revoke refresh token if it exists
        // Using ifPresent - don't fail if token doesn't exist (already logged out)
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                    log.info("Refresh token revoked for user: {}", token.getUser().getUsername());
                });
    }

    /**
     * Logout from ALL devices (revoke all user's refresh tokens).
     * 
     * Use cases:
     * - User changes password
     * - Account compromise suspected
     * - User requests "logout everywhere"
     */
    @Override
    @Transactional
    public void logoutAll(UserPrincipal userPrincipal) {
        log.info("Logging out user from all devices: {}", userPrincipal.getUsername());

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Bulk revoke all tokens for this user (single UPDATE query)
        refreshTokenRepository.revokeAllUserTokens(user, Instant.now());

        log.info("All refresh tokens revoked for user: {}", user.getUsername());
    }

    // ==================== USER OPERATIONS ====================

    /**
     * Get current authenticated user's profile.
     * 
     * readOnly = true: Optimization for read-only queries
     * - Hibernate won't flush changes
     * - Potential query cache usage
     */
    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UserPrincipal userPrincipal) {
        log.debug("Getting current user info: {}", userPrincipal.getUsername());

        // Load fresh user data from DB (UserPrincipal might have stale data)
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserDto(user);
    }

    /**
     * Change user's password with full validation.
     * 
     * Security measures:
     * 1. Verify current password is correct
     * 2. Ensure new password is different
     * 3. Confirm new password matches confirmation
     * 4. Revoke all refresh tokens (force re-login everywhere)
     */
    @Override
    @Transactional
    public void changePassword(UserPrincipal userPrincipal, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userPrincipal.getUsername());

        // VALIDATION 1: New password must match confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthException(
                    "New password and confirmation do not match",
                    AuthErrorCode.PASSWORD_MISMATCH
            );
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // VALIDATION 2: Current password must be correct
        // passwordEncoder.matches(rawPassword, encodedPassword)
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthException(
                    "Current password is incorrect",
                    AuthErrorCode.INVALID_CURRENT_PASSWORD
            );
        }

        // VALIDATION 3: New password must be different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AuthException(
                    "New password must be different from current password",
                    AuthErrorCode.PASSWORD_SAME_AS_OLD
            );
        }

        // Update password with new hash
        userRepository.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));

        // SECURITY: Force re-login on all devices
        // Password changed = all existing sessions should be invalidated
        refreshTokenRepository.revokeAllUserTokens(user, Instant.now());

        log.info("Password changed successfully for user: {}", user.getUsername());
    }

    // ==================== TOKEN VALIDATION ====================

    /**
     * Validate JWT access token.
     * 
     * Delegates to JwtTokenProvider for actual validation.
     * Used by /api/auth/validate endpoint for other services to verify tokens.
     */
    @Override
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    // ==================== HELPER METHODS ====================

    /*
     * Helper methods are private - internal implementation details.
     * Reduces code duplication and keeps public methods clean.
     */

    /**
     * Create and persist a new refresh token for user.
     * 
     * @param user The user to create token for
     * @return Persisted RefreshToken entity
     */
    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtTokenProvider.generateRefreshToken())  // Random UUID string
                .user(user)
                .expiresAt(jwtTokenProvider.getRefreshTokenExpiryDate())  // 7 days from now
                .revoked(false)  // Not yet revoked
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Build standardized AuthResponse with tokens and user info.
     * 
     * Used by register(), login(), refreshToken() for consistent responses.
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken, String message) {
        // Convert roles to string set (e.g., {"ROLE_USER", "ROLE_CUSTOMER"})
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")  // Standard OAuth2 token type
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())  // 86400 (24h)
                .expiresAt(Instant.now().plusSeconds(jwtTokenProvider.getAccessTokenExpirationSeconds()))
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .message(message)
                .build();
    }

    /**
     * Map User entity to UserDto (response object).
     * 
     * Excludes sensitive data (password).
     * Transforms roles from entities to string set.
     */
    private UserDto mapToUserDto(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .roles(roles)
                .enabled(user.isEnabled())
                .customerId(user.getCustomerId())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
