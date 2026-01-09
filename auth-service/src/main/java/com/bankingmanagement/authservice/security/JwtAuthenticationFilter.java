package com.bankingmanagement.authservice.security;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                         JWT AUTHENTICATION FILTER                                         ║
║                                                                                           ║
║  BUILD ORDER: STEP 7d of 12 (Fourth Security Component)                                  ║
║  PREVIOUS STEP: JwtTokenProvider (this filter uses it to validate tokens)                ║
║  NEXT STEP: JwtAuthenticationEntryPoint (handles when this filter rejects requests)      ║
║                                                                                           ║
║  THIS IS WHERE JWT TOKENS GET VALIDATED ON EVERY REQUEST                                 ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  WHAT IS A SERVLET FILTER?                                                               ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Filters intercept HTTP requests BEFORE they reach controllers.                     │ ║
║  │  They form a chain - each filter processes and passes to the next.                  │ ║
║  │                                                                                     │ ║
║  │  HTTP Request                                                                       │ ║
║  │       │                                                                             │ ║
║  │       ▼                                                                             │ ║
║  │  ┌─────────────────┐                                                                │ ║
║  │  │  Filter 1       │  <-- e.g., CORS filter                                         │ ║
║  │  └────────┬────────┘                                                                │ ║
║  │           ▼                                                                         │ ║
║  │  ┌─────────────────┐                                                                │ ║
║  │  │  Filter 2       │  <-- OUR JwtAuthenticationFilter                               │ ║
║  │  └────────┬────────┘                                                                │ ║
║  │           ▼                                                                         │ ║
║  │  ┌─────────────────┐                                                                │ ║
║  │  │  Filter 3...N   │  <-- Other security filters                                    │ ║
║  │  └────────┬────────┘                                                                │ ║
║  │           ▼                                                                         │ ║
║  │  ┌─────────────────┐                                                                │ ║
║  │  │  Controller     │  <-- Your @RestController                                      │ ║
║  │  └─────────────────┘                                                                │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY OncePerRequestFilter?                                                               ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Guarantees filter executes EXACTLY ONCE per request.                               │ ║
║  │                                                                                     │ ║
║  │  Problem: In some cases (forwards, includes), a filter might run multiple times.    │ ║
║  │  Solution: OncePerRequestFilter tracks if it already processed this request.        │ ║
║  │                                                                                     │ ║
║  │  For JWT: We want to validate token once, not multiple times per request.           │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  AUTHENTICATION FLOW IN THIS FILTER:                                                     ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  1. EXTRACT TOKEN                                                                   │ ║
║  │     Header: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."                         │ ║
║  │                              └───────────────────────────┘                          │ ║
║  │                                   Extract this part                                 │ ║
║  │                                                                                     │ ║
║  │  2. VALIDATE TOKEN (if present)                                                     │ ║
║  │     - Check signature (was it signed by our secret?)                                │ ║
║  │     - Check expiration (is it still valid?)                                         │ ║
║  │                                                                                     │ ║
║  │  3. LOAD USER (if token valid)                                                      │ ║
║  │     - Extract user ID from token's 'sub' claim                                      │ ║
║  │     - Load fresh UserDetails from database                                          │ ║
║  │     - Why reload? User might have been disabled, roles changed, etc.                │ ║
║  │                                                                                     │ ║
║  │  4. SET AUTHENTICATION                                                              │ ║
║  │     - Create Authentication object with UserDetails                                 │ ║
║  │     - Store in SecurityContextHolder                                                │ ║
║  │     - Now @PreAuthorize and other security checks work!                             │ ║
║  │                                                                                     │ ║
║  │  5. CONTINUE FILTER CHAIN                                                           │ ║
║  │     - Always call filterChain.doFilter() to pass to next filter                     │ ║
║  │     - Even if token invalid (let other filters/handlers deal with it)               │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHAT IS SecurityContextHolder?                                                          ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Spring Security's way of storing "who is the current user?"                        │ ║
║  │                                                                                     │ ║
║  │  SecurityContextHolder                                                              │ ║
║  │       └── SecurityContext                                                           │ ║
║  │              └── Authentication                                                     │ ║
║  │                     ├── Principal (UserPrincipal)                                   │ ║
║  │                     ├── Credentials (null for JWT - already validated)              │ ║
║  │                     └── Authorities (roles)                                         │ ║
║  │                                                                                     │ ║
║  │  Uses ThreadLocal: Each thread (request) has its own SecurityContext.               │ ║
║  │  After filter sets it, ANY code in this request can access current user:            │ ║
║  │                                                                                     │ ║
║  │  SecurityContextHolder.getContext().getAuthentication().getPrincipal()              │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT Authentication Filter - validates JWT on every request.
 * 
 * Extends OncePerRequestFilter to ensure single execution per request.
 * Registered in SecurityConfig's filter chain.
 * 
 * @Component makes this a Spring bean (auto-detected)
 * @RequiredArgsConstructor injects JwtTokenProvider and CustomUserDetailsService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /*
     * HTTP Header Constants
     * 
     * Authorization header format: "Bearer <token>"
     * - "Bearer " prefix indicates token-based authentication
     * - Followed by the actual JWT string
     * 
     * Example: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWI..."
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /*
     * Dependencies (injected via constructor)
     * - JwtTokenProvider: Validates token, extracts claims
     * - CustomUserDetailsService: Loads user from database by ID
     */
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Core filter logic - called for every HTTP request.
     * 
     * @param request  HTTP request (contains Authorization header)
     * @param response HTTP response (not modified here)
     * @param filterChain Chain to pass request to next filter
     * 
     * @NonNull annotation: Lombok generates null-check (defensive programming)
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // STEP 1: Extract JWT from "Authorization: Bearer <token>" header
            String jwt = extractTokenFromRequest(request);

            // STEP 2 & 3: Validate token and load user (if token present)
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                
                // Extract user ID from token's 'sub' claim
                UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);
                
                // Load fresh user data from database
                // Why not just use token data? User might have been disabled,
                // roles might have changed since token was issued
                UserDetails userDetails = userDetailsService.loadUserById(userId);

                /*
                 * STEP 4: Create Authentication object
                 * 
                 * UsernamePasswordAuthenticationToken is Spring's standard Authentication impl.
                 * Despite the name, we're using it for JWT (not username/password).
                 * 
                 * Constructor parameters:
                 * - principal: UserDetails (who is authenticated)
                 * - credentials: null (password not needed - already validated via JWT)
                 * - authorities: User's roles (for authorization checks)
                 */
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,         // Principal (UserPrincipal)
                                null,                // Credentials (not needed for JWT)
                                userDetails.getAuthorities()  // Roles
                        );

                /*
                 * Attach request details (IP address, session ID, etc.)
                 * Useful for audit logging and security tracking
                 */
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                /*
                 * CRITICAL: Set Authentication in SecurityContext
                 * 
                 * After this line:
                 * - @PreAuthorize annotations work
                 * - @AuthenticationPrincipal injects current user
                 * - SecurityContextHolder.getContext().getAuthentication() returns this
                 */
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set authentication for user: {}", userDetails.getUsername());
            }
            // If no token or invalid token: SecurityContext remains empty
            // This is fine for public endpoints (/api/auth/login, /api/auth/register)
            // Protected endpoints will be rejected by Spring Security later
            
        } catch (Exception ex) {
            // Log but don't throw - let request continue
            // Spring Security will handle unauthenticated requests appropriately
            log.error("Cannot set user authentication: {}", ex.getMessage());
        }

        // STEP 5: ALWAYS continue the filter chain
        // Even if authentication failed, pass to next filter
        // Let SecurityConfig's rules decide what to do with unauthenticated requests
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     * 
     * Expected format: "Bearer eyJhbGciOiJIUzI1NiJ9..."
     *                        └─────── 7 characters prefix
     * 
     * @param request HTTP request containing headers
     * @return JWT string (without "Bearer " prefix), or null if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        // Check if header exists and starts with "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            // Remove "Bearer " prefix (7 characters)
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // No token found - this is OK for public endpoints
        return null;
    }
}
