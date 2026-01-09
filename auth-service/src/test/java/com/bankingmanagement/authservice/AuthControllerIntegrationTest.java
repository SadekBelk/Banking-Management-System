package com.bankingmanagement.authservice;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                        AUTH CONTROLLER INTEGRATION TEST                                   ║
║                                                                                           ║
║  BUILD ORDER: STEP 12 of 12 (Testing - Final Step!)                                       ║
║  PREVIOUS STEP: HTTP request files (manual testing)                                       ║
║  THIS IS THE FINAL STEP - Automated test verification                                     ║
║                                                                                           ║
║  PURPOSE: Verify auth-service endpoints work correctly end-to-end                         ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  TEST PYRAMID:                                                                           ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │                           /\                                                        │ ║
║  │                          /  \        E2E Tests (few)                                │ ║
║  │                         /    \       - Full system tests                            │ ║
║  │                        /______\                                                     │ ║
║  │                       /        \                                                    │ ║
║  │                      / INTEGR-  \    Integration Tests ← THIS FILE                  │ ║
║  │                     /   ATION    \   - Controller + Service + Repository            │ ║
║  │                    /______________\  - Real database (H2 in-memory)                 │ ║
║  │                   /                \                                                │ ║
║  │                  /    UNIT TESTS    \ Unit Tests (many)                             │ ║
║  │                 /____________________\ - Single class in isolation                  │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHAT THIS FILE TESTS:                                                                   ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  @SpringBootTest - Loads full Spring context (all beans)                            │ ║
║  │  @AutoConfigureMockMvc - Provides MockMvc for HTTP testing                          │ ║
║  │  @ActiveProfiles("local") - Uses H2 database (not PostgreSQL)                       │ ║
║  │                                                                                     │ ║
║  │  Tests the complete request flow:                                                   │ ║
║  │                                                                                     │ ║
║  │  HTTP Request                                                                       │ ║
║  │       │                                                                            │ ║
║  │       ▼                                                                            │ ║
║  │  MockMvc (simulates HTTP client)                                                    │ ║
║  │       │                                                                            │ ║
║  │       ▼                                                                            │ ║
║  │  SecurityFilterChain (JwtAuthenticationFilter, etc.)                                │ ║
║  │       │                                                                            │ ║
║  │       ▼                                                                            │ ║
║  │  AuthController (REST endpoints)                                                    │ ║
║  │       │                                                                            │ ║
║  │       ▼                                                                            │ ║
║  │  AuthService (business logic)                                                       │ ║
║  │       │                                                                            │ ║
║  │       ▼                                                                            │ ║
║  │  Repository (JPA)                                                                   │ ║
║  │       │                                                                            │ ║
║  │       ▼                                                                            │ ║
║  │  H2 Database (in-memory)                                                            │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  RUN TESTS:                                                                              ║
║  - IDE: Right-click → Run                                                                ║
║  - Command: cd auth-service && mvn test                                                  ║
║  - Specific test: mvn test -Dtest=AuthControllerIntegrationTest                          ║
║                                                                                           ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.dto.LoginRequest;
import com.bankingmanagement.authservice.dto.RegisterRequest;
import com.bankingmanagement.authservice.entity.Role;
import com.bankingmanagement.authservice.repository.RoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * 
 * Annotations explained:
 * 
 * @SpringBootTest
 * - Loads complete Spring application context
 * - All beans are created (controllers, services, repositories)
 * - Uses real configuration (application-local.yml for "local" profile)
 * 
 * @AutoConfigureMockMvc
 * - Provides MockMvc bean for HTTP testing
 * - Simulates HTTP requests without starting real HTTP server
 * - Faster than @WebMvcTest (no separate server process)
 * 
 * @ActiveProfiles("local")
 * - Uses application-local.yml configuration
 * - H2 in-memory database (not PostgreSQL)
 * - Each test gets fresh database (auto-created tables)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")  // Uses H2 database from application-local.yml
class AuthControllerIntegrationTest {

    /*
     * MockMvc - Spring's HTTP test client
     * Simulates HTTP requests to the controller
     * No actual network calls - stays within JVM
     */
    @Autowired
    private MockMvc mockMvc;

    /*
     * ObjectMapper - JSON serialization/deserialization
     * Same one Spring Boot uses for REST endpoints
     */
    @Autowired
    private ObjectMapper objectMapper;

    /*
     * RoleRepository - Direct database access for test setup
     * Need to ensure roles exist before registration tests
     */
    @Autowired
    private RoleRepository roleRepository;

    private static final String BASE_URL = "/api/auth";

    /**
     * Runs before each test method.
     * Ensures ROLE_USER exists in database.
     * 
     * Why? Registration assigns ROLE_USER to new users.
     * Without this role, registration would fail.
     */
    @BeforeEach
    void setUp() {
        // Idempotent: only creates if doesn't exist
        if (!roleRepository.existsByName(Role.RoleName.ROLE_USER)) {
            roleRepository.save(Role.builder()
                    .name(Role.RoleName.ROLE_USER)
                    .description("Basic user")
                    .build());
        }
    }

    // ==================== REGISTRATION TESTS ====================

    /**
     * Test: Successful user registration
     * 
     * Verifies:
     * - 201 Created status
     * - Access token returned
     * - Refresh token returned
     * - Token type is "Bearer"
     * - Username and email match request
     * - ROLE_USER assigned by default
     */
    @Test
    @DisplayName("Should register a new user successfully")
    void shouldRegisterNewUser() throws Exception {
        // Use timestamp to ensure unique username/email per test run
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser" + System.currentTimeMillis())
                .email("test" + System.currentTimeMillis() + "@example.com")
                .password("TestPass@123")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())                           // HTTP 201
                .andExpect(jsonPath("$.accessToken", notNullValue()))      // JWT returned
                .andExpect(jsonPath("$.refreshToken", notNullValue()))     // Refresh token returned
                .andExpect(jsonPath("$.tokenType", is("Bearer")))          // Standard Bearer type
                .andExpect(jsonPath("$.username", is(request.getUsername())))
                .andExpect(jsonPath("$.email", is(request.getEmail())))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")));     // Default role assigned
    }

    /**
     * Test: Registration validation failure
     * 
     * Verifies that weak password triggers validation error.
     * Password requirements: 8+ chars, uppercase, lowercase, digit, special char
     */
    @Test
    @DisplayName("Should fail registration with invalid password")
    void shouldFailRegistrationWithInvalidPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser2")
                .email("test2@example.com")
                .password("weak")  // Too weak - fails @Size(min=8) constraint
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())                        // HTTP 400
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0)))); // Validation errors
    }

    // ==================== LOGIN TESTS ====================

    /**
     * Test: Successful login flow
     * 
     * Steps:
     * 1. Register a new user (to ensure credentials exist)
     * 2. Login with same credentials
     * 3. Verify tokens returned
     * 
     * This tests the complete auth flow: registration → login
     */
    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        // SETUP: Create a user first
        String username = "loginuser" + System.currentTimeMillis();
        String email = "login" + System.currentTimeMillis() + "@example.com";
        String password = "LoginPass@123";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // TEST: Login with created credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail(username)  // Can also use email
                .password(password)
                .build();

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())                           // HTTP 200
                .andExpect(jsonPath("$.accessToken", notNullValue())) // JWT returned
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.username", is(username)));
    }

    /**
     * Test: Login with invalid credentials
     * 
     * Verifies 401 Unauthorized for non-existent user
     */
    @Test
    @DisplayName("Should fail login with invalid credentials")
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("nonexistent")
                .password("wrongpass")
                .build();

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());  // HTTP 401
    }

    // ==================== PROTECTED ENDPOINT TESTS ====================

    /**
     * Test: Access /me endpoint with valid JWT
     * 
     * Steps:
     * 1. Register user and capture access token
     * 2. Call /me with token in Authorization header
     * 3. Verify user info returned
     * 
     * This tests the JWT authentication filter works correctly.
     */
    @Test
    @DisplayName("Should get current user with valid token")
    void shouldGetCurrentUserWithValidToken() throws Exception {
        // SETUP: Register and capture token
        String username = "meuser" + System.currentTimeMillis();
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(username)
                .email("me" + System.currentTimeMillis() + "@example.com")
                .password("MePass@123")
                .build();

        MvcResult result = mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract access token from response
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();

        // TEST: Call protected endpoint with token
        mockMvc.perform(get(BASE_URL + "/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)));
    }

    /**
     * Test: Access protected endpoint WITHOUT token
     * 
     * Verifies security filter rejects unauthenticated requests.
     * JwtAuthenticationEntryPoint returns 401.
     */
    @Test
    @DisplayName("Should fail to access protected endpoint without token")
    void shouldFailAccessWithoutToken() throws Exception {
        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isUnauthorized());  // HTTP 401
    }

    // ==================== TOKEN VALIDATION TESTS ====================

    /**
     * Test: Token validation endpoint
     * 
     * Tests both valid and invalid token scenarios.
     * This endpoint is used by other services to verify tokens.
     */
    @Test
    @DisplayName("Should validate token endpoint")
    void shouldValidateToken() throws Exception {
        // SETUP: Register and get token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("validateuser" + System.currentTimeMillis())
                .email("validate" + System.currentTimeMillis() + "@example.com")
                .password("ValidPass@123")
                .build();

        MvcResult result = mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();

        // TEST: Valid token should return valid=true
        mockMvc.perform(get(BASE_URL + "/validate")
                        .param("token", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)));

        // TEST: Invalid token should return valid=false
        mockMvc.perform(get(BASE_URL + "/validate")
                        .param("token", "invalid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));
    }
}

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                           ║
║  🎉🎉🎉 AUTH SERVICE BUILD COMPLETE! 🎉🎉🎉                                              ║
║                                                                                           ║
║  COMPLETE BUILD ORDER (12 Steps):                                                        ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  FOUNDATION (Steps 1-2):                                                            │ ║
║  │  ✅ Step 1:  pom.xml - Dependencies and build configuration                         │ ║
║  │  ✅ Step 2:  application.yml - Database, JWT, server settings                       │ ║
║  │                                                                                     │ ║
║  │  DATA LAYER (Steps 3-4):                                                            │ ║
║  │  ✅ Step 3:  Entities (User.java, Role.java, RefreshToken.java)                     │ ║
║  │  ✅ Step 4:  Repositories (UserRepository, RoleRepository, RefreshTokenRepository)  │ ║
║  │                                                                                     │ ║
║  │  API CONTRACTS (Step 5):                                                            │ ║
║  │  ✅ Step 5:  DTOs (RegisterRequest, LoginRequest, AuthResponse, etc.)               │ ║
║  │                                                                                     │ ║
║  │  ERROR HANDLING (Step 6):                                                           │ ║
║  │  ✅ Step 6:  Exceptions + GlobalExceptionHandler                                    │ ║
║  │                                                                                     │ ║
║  │  SECURITY CORE (Step 7):                                                            │ ║
║  │  ✅ Step 7a: UserPrincipal (Spring Security principal)                              │ ║
║  │  ✅ Step 7b: CustomUserDetailsService                                               │ ║
║  │  ✅ Step 7c: JwtTokenProvider (JWT generation/validation)                           │ ║
║  │  ✅ Step 7d: JwtAuthenticationFilter                                                │ ║
║  │  ✅ Step 7e: JwtAuthenticationEntryPoint (401 handler)                              │ ║
║  │  ✅ Step 7f: JwtAccessDeniedHandler (403 handler)                                   │ ║
║  │                                                                                     │ ║
║  │  BUSINESS LOGIC (Step 8):                                                           │ ║
║  │  ✅ Step 8a: AuthService interface                                                  │ ║
║  │  ✅ Step 8b: AuthServiceImpl                                                        │ ║
║  │                                                                                     │ ║
║  │  CONFIGURATION (Step 9):                                                            │ ║
║  │  ✅ Step 9a: SecurityConfig (filter chain)                                          │ ║
║  │  ✅ Step 9b: DataInitializer (default roles)                                        │ ║
║  │  ✅ Step 9c: OpenApiConfig (Swagger)                                                │ ║
║  │                                                                                     │ ║
║  │  HTTP LAYER (Step 10):                                                              │ ║
║  │  ✅ Step 10: AuthController (REST endpoints)                                        │ ║
║  │                                                                                     │ ║
║  │  DEPLOYMENT (Step 11):                                                              │ ║
║  │  ✅ Step 11a: Dockerfile                                                            │ ║
║  │  ✅ Step 11b: docker-compose.yml                                                    │ ║
║  │                                                                                     │ ║
║  │  TESTING (Step 12):                                                                 │ ║
║  │  ✅ Step 12: Integration tests + HTTP request files                                 │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY THIS ORDER?                                                                         ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  Dependencies flow BOTTOM-UP:                                                       │ ║
║  │                                                                                     │ ║
║  │       Controller ─────────► Service ─────────► Repository                           │ ║
║  │           │                    │                    │                              │ ║
║  │           │                    │                    │                              │ ║
║  │           ▼                    ▼                    ▼                              │ ║
║  │         DTOs              Security            Entities                             │ ║
║  │                          (JWT, Filter)                                             │ ║
║  │                               │                                                    │ ║
║  │                               ▼                                                    │ ║
║  │                           pom.xml                                                  │ ║
║  │                        (dependencies)                                              │ ║
║  │                                                                                     │ ║
║  │  You can't build a layer without its dependencies in place!                        │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/
