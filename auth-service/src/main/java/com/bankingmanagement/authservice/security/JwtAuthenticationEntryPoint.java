package com.bankingmanagement.authservice.security;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                       JWT AUTHENTICATION ENTRY POINT                                      ║
║                                                                                           ║
║  BUILD ORDER: STEP 7e of 12 (Fifth Security Component)                                   ║
║  PREVIOUS STEP: JwtAuthenticationFilter (this handles when filter doesn't authenticate)  ║
║  NEXT STEP: JwtAccessDeniedHandler (handles different error - 403 vs 401)                ║
║                                                                                           ║
║  HANDLES: 401 UNAUTHORIZED RESPONSES                                                     ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  WHAT IS AuthenticationEntryPoint?                                                       ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Spring Security interface for handling UNAUTHENTICATED access attempts.            │ ║
║  │                                                                                     │ ║
║  │  WHEN IS THIS CALLED?                                                               │ ║
║  │  - User tries to access protected endpoint WITHOUT a valid JWT token                │ ║
║  │  - JWT token is missing, expired, or invalid                                        │ ║
║  │  - JwtAuthenticationFilter didn't set Authentication in SecurityContext             │ ║
║  │                                                                                     │ ║
║  │  EXAMPLE SCENARIO:                                                                  │ ║
║  │  1. Client calls: GET /api/auth/me (protected endpoint)                             │ ║
║  │  2. No Authorization header present                                                 │ ║
║  │  3. JwtAuthenticationFilter: "No token, SecurityContext stays empty"                │ ║
║  │  4. Spring Security: "Protected endpoint, but no Authentication!"                   │ ║
║  │  5. AuthenticationEntryPoint.commence() is called                                   │ ║
║  │  6. We return 401 Unauthorized with JSON error body                                 │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  401 UNAUTHORIZED vs 403 FORBIDDEN:                                                      ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  401 UNAUTHORIZED (AuthenticationEntryPoint):                                       │ ║
║  │  "I don't know who you are. Please authenticate."                                   │ ║
║  │  - No token provided                                                                │ ║
║  │  - Invalid/expired token                                                            │ ║
║  │  - Client should: Redirect to login or refresh token                                │ ║
║  │                                                                                     │ ║
║  │  403 FORBIDDEN (AccessDeniedHandler):                                               │ ║
║  │  "I know who you are, but you're not allowed."                                      │ ║
║  │  - Valid token, but wrong role                                                      │ ║
║  │  - User doesn't have required permission                                            │ ║
║  │  - Client should: Show "permission denied" message                                  │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY CUSTOM ENTRY POINT? (vs Spring's default)                                           ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  DEFAULT BEHAVIOR:                                                                  │ ║
║  │  - Traditional web apps: Redirect to /login page (HTTP 302)                         │ ║
║  │  - Not suitable for REST APIs - clients expect JSON response                        │ ║
║  │                                                                                     │ ║
║  │  OUR CUSTOM BEHAVIOR:                                                               │ ║
║  │  - Return HTTP 401 status                                                           │ ║
║  │  - Return JSON error body matching our ApiError format                              │ ║
║  │  - Consistent with other API error responses                                        │ ║
║  │  - Frontend can parse and display appropriate message                               │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.exception.ApiError;
import com.bankingmanagement.authservice.exception.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Custom AuthenticationEntryPoint for handling unauthorized requests.
 * Returns JSON error response instead of redirecting to login page.
 * 
 * Registered in SecurityConfig:
 *   .exceptionHandling(ex -> ex.authenticationEntryPoint(this))
 * 
 * @Component makes this a Spring bean (auto-detected and injectable)
 * @RequiredArgsConstructor injects ObjectMapper for JSON serialization
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /*
     * Jackson ObjectMapper - serializes ApiError to JSON
     * Auto-configured by Spring Boot (jackson-databind in classpath)
     */
    private final ObjectMapper objectMapper;

    /**
     * Handle authentication failure - user not authenticated.
     * 
     * Called by Spring Security when:
     * - Protected endpoint accessed without valid credentials
     * - AuthenticationException thrown during authentication
     * 
     * @param request The HTTP request that failed authentication
     * @param response HTTP response to write error to
     * @param authException The exception that triggered this (contains details)
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        // Log for debugging/monitoring (WARN level - expected but notable)
        log.warn("Unauthorized request to '{}': {}", request.getRequestURI(), authException.getMessage());

        /*
         * Build ApiError response matching our standard error format
         * Same format used by GlobalExceptionHandler for consistency
         * 
         * Fields:
         * - timestamp: When error occurred
         * - status: HTTP status code (401)
         * - error: HTTP status text ("Unauthorized")
         * - code: Our error code (AUTH_001)
         * - message: Human-readable explanation
         * - path: Endpoint that was requested
         */
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())  // 401
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())  // "Unauthorized"
                .code(AuthErrorCode.AUTHENTICATION_FAILED.getCode())  // "AUTH_001"
                .message("Full authentication is required to access this resource")
                .path(request.getRequestURI())
                .build();

        // Set response headers
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);  // "application/json"
        response.setStatus(HttpStatus.UNAUTHORIZED.value());  // 401
        
        // Write JSON body directly to response output stream
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
