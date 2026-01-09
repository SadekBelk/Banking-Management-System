package com.bankingmanagement.authservice.controller;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                              AUTH CONTROLLER                                              ║
║                                                                                           ║
║  BUILD ORDER: STEP 10 of 12 (REST Endpoints - The Public Interface)                      ║
║  PREVIOUS STEP: Configuration classes (security, OpenAPI all wired up)                   ║
║  NEXT STEP: Docker configuration (containerize the service)                              ║
║                                                                                           ║
║  THIS IS THE ENTRY POINT FOR ALL HTTP REQUESTS                                           ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  REST CONTROLLER PATTERN:                                                                ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  HTTP Request → Controller → Service → Repository → Database                        │ ║
║  │                                                                                     │ ║
║  │  Controller Responsibilities (THIN controller):                                     │ ║
║  │  • HTTP handling (request/response)                                                 │ ║
║  │  • Input validation (via @Valid)                                                    │ ║
║  │  • Response status codes                                                            │ ║
║  │  • API documentation (Swagger annotations)                                          │ ║
║  │  • Logging                                                                          │ ║
║  │                                                                                     │ ║
║  │  Service Responsibilities (THICK service):                                          │ ║
║  │  • Business logic                                                                   │ ║
║  │  • Transaction management                                                           │ ║
║  │  • Complex operations                                                               │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  ENDPOINT MAP:                                                                           ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  PUBLIC ENDPOINTS (no auth required):                                               │ ║
║  │  ┌──────────────────────────────────────────────────────────────────────────────┐  │ ║
║  │  │  POST /api/auth/register  - Create new account, get tokens                   │  │ ║
║  │  │  POST /api/auth/login     - Login with credentials, get tokens               │  │ ║
║  │  │  POST /api/auth/refresh   - Exchange refresh token for new access token      │  │ ║
║  │  │  GET  /api/auth/validate  - Check if access token is valid                   │  │ ║
║  │  └──────────────────────────────────────────────────────────────────────────────┘  │ ║
║  │                                                                                     │ ║
║  │  PROTECTED ENDPOINTS (JWT required):                                                │ ║
║  │  ┌──────────────────────────────────────────────────────────────────────────────┐  │ ║
║  │  │  POST /api/auth/logout     - Revoke refresh token (single device)            │  │ ║
║  │  │  POST /api/auth/logout-all - Revoke all refresh tokens (all devices)         │  │ ║
║  │  │  GET  /api/auth/me         - Get current user profile                        │  │ ║
║  │  │  POST /api/auth/change-password - Change user password                       │  │ ║
║  │  └──────────────────────────────────────────────────────────────────────────────┘  │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  KEY ANNOTATIONS:                                                                        ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  @RestController = @Controller + @ResponseBody                                      │ ║
║  │  - All methods return JSON (not view names)                                         │ ║
║  │                                                                                     │ ║
║  │  @RequestMapping("/api/auth") - Base path for all endpoints                         │ ║
║  │                                                                                     │ ║
║  │  @Valid - Triggers Jakarta Bean Validation on request body                          │ ║
║  │  - @NotBlank, @Email, @Size constraints checked                                     │ ║
║  │  - Invalid → MethodArgumentNotValidException → GlobalExceptionHandler               │ ║
║  │                                                                                     │ ║
║  │  @AuthenticationPrincipal UserPrincipal - Injects current user from SecurityContext │ ║
║  │  - Populated by JwtAuthenticationFilter                                             │ ║
║  │  - Null if not authenticated (for protected endpoints, security rejects first)      │ ║
║  │                                                                                     │ ║
║  │  @SecurityRequirement(name = "bearerAuth") - Swagger: needs JWT                     │ ║
║  │  - Shows 🔒 icon in Swagger UI                                                      │ ║
║  │  - Name must match OpenApiConfig security scheme                                    │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.dto.*;
import com.bankingmanagement.authservice.security.UserPrincipal;
import com.bankingmanagement.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for authentication endpoints.
 *
 * This is the HTTP interface for authentication operations.
 * All business logic is delegated to AuthService.
 * 
 * @RestController - Returns JSON responses (not views)
 * @RequestMapping("/api/auth") - Base URL prefix for all endpoints
 * @RequiredArgsConstructor - Constructor injection of AuthService
 * @Slf4j - Logging via log.info(), log.debug()
 * @Tag - Swagger grouping: "Authentication" section in docs
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    /*
     * Service dependency - all business logic delegated here.
     * Injected via constructor (RequiredArgsConstructor).
     */
    private final AuthService authService;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Register a new user.
     * 
     * PUBLIC ENDPOINT - No authentication required.
     * 
     * Request body validated by @Valid against RegisterRequest constraints:
     * - username: @NotBlank, @Size(4-50)
     * - email: @NotBlank, @Email
     * - password: @NotBlank, @Size(8-100)
     * 
     * @param request Registration details (validated)
     * @return AuthResponse with tokens (201 Created)
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns JWT tokens"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or username/email already exists"
            )
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("Registration request for username: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        // 201 Created - new resource created
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and return tokens.
     * 
     * PUBLIC ENDPOINT - No authentication required.
     * 
     * Accepts username OR email (handled by CustomUserDetailsService).
     * 
     * @param request Login credentials
     * @return AuthResponse with tokens (200 OK)
     */
    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates user with username/email and password, returns JWT tokens"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            )
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        log.info("Login request for: {}", request.getUsernameOrEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token.
     * 
     * PUBLIC ENDPOINT - No JWT required (uses refresh token instead).
     * 
     * Token Rotation: Old refresh token is revoked, new one issued.
     * 
     * @param request Contains refresh token
     * @return AuthResponse with new tokens (200 OK)
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Uses refresh token to obtain a new access token"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token"
            )
    })
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("Token refresh request");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    // ==================== PROTECTED ENDPOINTS ====================

    /**
     * Logout user and revoke refresh token.
     * 
     * Revokes single refresh token (single device logout).
     * Access token remains valid until expiry (stateless limitation).
     * 
     * @param request Contains refresh token to revoke
     * @return Success message (200 OK)
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Revokes the provided refresh token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("Logout request");
        authService.logout(request.getRefreshToken());
        // Return simple JSON object: {"message": "Logout successful"}
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    /**
     * Logout from all devices (revoke all refresh tokens).
     * 
     * PROTECTED ENDPOINT - JWT required.
     * @SecurityRequirement(name = "bearerAuth") - Shows 🔒 in Swagger UI
     * @AuthenticationPrincipal - Injects UserPrincipal from SecurityContext
     * 
     * Revokes ALL refresh tokens for this user (all devices).
     * 
     * HOW @AuthenticationPrincipal WORKS:
     * ┌────────────────────────────────────────────────────────────────────────────┐
     * │  HTTP Request with Authorization: Bearer <token>                          │
     * │         │                                                                 │
     * │         ▼                                                                 │
     * │  JwtAuthenticationFilter                                                  │
     * │  • Extracts token from header                                             │
     * │  • Validates signature & expiry                                           │
     * │  • Loads UserPrincipal from database                                      │
     * │  • Creates UsernamePasswordAuthenticationToken(userPrincipal, ...)        │
     * │  • Stores in SecurityContextHolder                                        │
     * │         │                                                                 │
     * │         ▼                                                                 │
     * │  Spring Security Resolver                                                 │
     * │  • Sees @AuthenticationPrincipal UserPrincipal userPrincipal              │
     * │  • Gets Authentication from SecurityContextHolder                         │
     * │  • Extracts principal: authentication.getPrincipal()                      │
     * │  • Injects UserPrincipal into method parameter                            │
     * │         │                                                                 │
     * │         ▼                                                                 │
     * │  Controller method receives fully populated UserPrincipal                 │
     * └────────────────────────────────────────────────────────────────────────────┘
     * 
     * @param userPrincipal Injected from SecurityContext (JWT → UserPrincipal)
     * @return Success message (200 OK)
     */
    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth") // Swagger: requires JWT, shows 🔒 icon
    @Operation(
            summary = "Logout from all devices",
            description = "Revokes all refresh tokens for the current user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out from all devices"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> logoutAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Logout all request for user: {}", userPrincipal.getUsername());
        authService.logoutAll(userPrincipal);
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }

    /**
     * Get current user information.
     * 
     * PROTECTED ENDPOINT - JWT required.
     * Returns user profile WITHOUT password hash.
     * 
     * @param userPrincipal Injected from SecurityContext
     * @return UserDto with profile data (200 OK)
     */
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth") // Swagger: requires JWT
    @Operation(
            summary = "Get current user",
            description = "Returns information about the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User info retrieved",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserDto> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.debug("Get current user request: {}", userPrincipal.getUsername());
        UserDto user = authService.getCurrentUser(userPrincipal);
        return ResponseEntity.ok(user);
    }

    /**
     * Change user password.
     * 
     * PROTECTED ENDPOINT - JWT required.
     * 
     * SECURITY NOTE: Even though user is authenticated,
     * we require current password verification. This protects against:
     * - Stolen JWT tokens
     * - Unattended sessions
     * - Session hijacking
     * 
     * @param userPrincipal Injected from SecurityContext
     * @param request Current password + new password
     * @return Success message recommending re-login (200 OK)
     */
    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth") // Swagger: requires JWT
    @Operation(
            summary = "Change password",
            description = "Changes the password for the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password or validation error"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("Change password request for user: {}", userPrincipal.getUsername());
        authService.changePassword(userPrincipal, request);
        // Recommend re-login for security (old JWTs still work until expiry)
        return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully. Please login again with your new password."
        ));
    }

    // ==================== VALIDATION ENDPOINT ====================

    /**
     * Validate access token.
     * 
     * PUBLIC ENDPOINT - Used by other services to verify tokens.
     * 
     * NOTE: This takes token as query parameter (not header).
     * Different from typical usage where token is in Authorization header.
     * 
     * Use case: API Gateway or other services call this to verify
     * a token before forwarding requests.
     * 
     * @param token JWT access token to validate
     * @return Validation result with valid/invalid status (200 OK)
     */
    @GetMapping("/validate")
    @Operation(
            summary = "Validate token",
            description = "Validates if the provided access token is valid"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token validation result")
    })
    public ResponseEntity<Map<String, Object>> validateToken(
            @Parameter(description = "JWT access token to validate")
            @RequestParam String token
    ) {
        log.debug("Token validation request");
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "message", valid ? "Token is valid" : "Token is invalid or expired"
        ));
    }
}

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                           REQUEST/RESPONSE FLOW SUMMARY                                   ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  PUBLIC ENDPOINT FLOW (e.g., /login):                                                    ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  Client                                                                             │ ║
║  │    │                                                                               │ ║
║  │    ├──POST /api/auth/login─────────────────────────────────────────────────►       │ ║
║  │    │  {"usernameOrEmail": "john", "password": "secret"}                             │ ║
║  │    │                                                                               │ ║
║  │    │                    SecurityFilterChain                                         │ ║
║  │    │                           │                                                   │ ║
║  │    │                    JwtAuthenticationFilter                                     │ ║
║  │    │                    (no token, skips)                                          │ ║
║  │    │                           │                                                   │ ║
║  │    │                    AuthController.login()                                      │ ║
║  │    │                           │                                                   │ ║
║  │    │                    AuthServiceImpl.login()                                     │ ║
║  │    │                    • Find user by username/email                              │ ║
║  │    │                    • Verify password (BCrypt)                                 │ ║
║  │    │                    • Generate JWT tokens                                       │ ║
║  │    │                    • Save refresh token to DB                                  │ ║
║  │    │                           │                                                   │ ║
║  │    ◄──────────────────────────────────────────────────────────────────────────────  │ ║
║  │    200 OK                                                                           │ ║
║  │    {                                                                               │ ║
║  │      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",                             │ ║
║  │      "refreshToken": "a7f3bc8e-2d1a-4e5c-9876...",                                  │ ║
║  │      "tokenType": "Bearer",                                                         │ ║
║  │      "expiresIn": 86400                                                            │ ║
║  │    }                                                                               │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  PROTECTED ENDPOINT FLOW (e.g., /me):                                                    ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  Client                                                                             │ ║
║  │    │                                                                               │ ║
║  │    ├──GET /api/auth/me─────────────────────────────────────────────────────────►   │ ║
║  │    │  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6...                         │ ║
║  │    │                                                                               │ ║
║  │    │                    SecurityFilterChain                                         │ ║
║  │    │                           │                                                   │ ║
║  │    │                    JwtAuthenticationFilter                                     │ ║
║  │    │                    • Extract token from header                                │ ║
║  │    │                    • Validate signature & expiry                              │ ║
║  │    │                    • Extract userId from token                                │ ║
║  │    │                    • Load UserPrincipal from DB                               │ ║
║  │    │                    • Set SecurityContext                                       │ ║
║  │    │                           │                                                   │ ║
║  │    │                    AuthController.getCurrentUser()                             │ ║
║  │    │                    @AuthenticationPrincipal UserPrincipal userPrincipal        │ ║
║  │    │                    (injected from SecurityContext)                             │ ║
║  │    │                           │                                                   │ ║
║  │    │                    AuthServiceImpl.getCurrentUser()                            │ ║
║  │    │                    • Load full user from DB                                    │ ║
║  │    │                    • Map to UserDto                                            │ ║
║  │    │                           │                                                   │ ║
║  │    ◄──────────────────────────────────────────────────────────────────────────────  │ ║
║  │    200 OK                                                                           │ ║
║  │    {                                                                               │ ║
║  │      "id": "uuid",                                                                 │ ║
║  │      "username": "john_doe",                                                       │ ║
║  │      "email": "john@example.com",                                                  │ ║
║  │      "roles": ["ROLE_USER", "ROLE_CUSTOMER"]                                       │ ║
║  │    }                                                                               │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  STEP 10 COMPLETE!                                                                       ║
║  NEXT: Step 11 - Docker configuration (Dockerfile, docker-compose.yml)                   ║
║                                                                                           ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/
