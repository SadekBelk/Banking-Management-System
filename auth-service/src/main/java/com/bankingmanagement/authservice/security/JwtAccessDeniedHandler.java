package com.bankingmanagement.authservice.security;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                          JWT ACCESS DENIED HANDLER                                        ║
║                                                                                           ║
║  BUILD ORDER: STEP 7f of 12 (Sixth/Last Security Component)                              ║
║  PREVIOUS STEP: JwtAuthenticationEntryPoint (handles 401, this handles 403)              ║
║  NEXT STEP: Service Layer (AuthService interface and implementation)                     ║
║                                                                                           ║
║  HANDLES: 403 FORBIDDEN RESPONSES                                                        ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  WHAT IS AccessDeniedHandler?                                                            ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Spring Security interface for handling AUTHORIZATION failures.                     │ ║
║  │                                                                                     │ ║
║  │  WHEN IS THIS CALLED?                                                               │ ║
║  │  - User IS authenticated (valid JWT token)                                          │ ║
║  │  - But user LACKS required permission/role                                          │ ║
║  │  - AccessDeniedException is thrown                                                  │ ║
║  │                                                                                     │ ║
║  │  EXAMPLE SCENARIO:                                                                  │ ║
║  │  1. User with ROLE_USER calls: DELETE /api/admin/users/123                          │ ║
║  │  2. JWT token is valid, user is authenticated                                       │ ║
║  │  3. Endpoint requires ROLE_ADMIN                                                    │ ║
║  │  4. Spring Security: "User doesn't have ROLE_ADMIN!"                                │ ║
║  │  5. AccessDeniedHandler.handle() is called                                          │ ║
║  │  6. We return 403 Forbidden with JSON error body                                    │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  AUTHENTICATION vs AUTHORIZATION:                                                        ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  AUTHENTICATION (Who are you?):                                                     │ ║
║  │  - Handled by JwtAuthenticationFilter                                               │ ║
║  │  - Validates JWT token                                                              │ ║
║  │  - Sets SecurityContext                                                             │ ║
║  │  - Failure → 401 Unauthorized (AuthenticationEntryPoint)                            │ ║
║  │                                                                                     │ ║
║  │  AUTHORIZATION (What can you do?):                                                  │ ║
║  │  - Handled after authentication                                                     │ ║
║  │  - Checks roles/permissions                                                         │ ║
║  │  - Based on @PreAuthorize, .hasRole(), etc.                                         │ ║
║  │  - Failure → 403 Forbidden (AccessDeniedHandler)                                    │ ║
║  │                                                                                     │ ║
║  │  ┌────────────────────────────────────────────────────────────────────────────┐     │ ║
║  │  │ "You can be WHO you claim to be (authenticated)                            │     │ ║
║  │  │  but still not ALLOWED to do what you want (unauthorized)"                 │     │ ║
║  │  └────────────────────────────────────────────────────────────────────────────┘     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  AUTHORIZATION METHODS THAT TRIGGER THIS:                                                ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  1. URL-based (in SecurityConfig):                                                  │ ║
║  │     .requestMatchers("/api/admin/**").hasRole("ADMIN")                              │ ║
║  │                                                                                     │ ║
║  │  2. Method-level (on controller/service methods):                                   │ ║
║  │     @PreAuthorize("hasRole('ADMIN')")                                               │ ║
║  │     @PreAuthorize("hasAuthority('ROLE_MANAGER')")                                   │ ║
║  │     @Secured("ROLE_ADMIN")                                                          │ ║
║  │                                                                                     │ ║
║  │  3. SpEL expressions:                                                               │ ║
║  │     @PreAuthorize("#userId == authentication.principal.id")                         │ ║
║  │     (Only allow users to access their own data)                                     │ ║
║  │                                                                                     │ ║
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Custom AccessDeniedHandler for handling forbidden requests.
 * Returns JSON error response for insufficient permissions.
 * 
 * Registered in SecurityConfig:
 *   .exceptionHandling(ex -> ex.accessDeniedHandler(this))
 * 
 * This is the LAST LINE OF DEFENSE - user is authenticated but not authorized.
 * 
 * @Component makes this a Spring bean (auto-detected and injectable)
 * @RequiredArgsConstructor injects ObjectMapper for JSON serialization
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    /*
     * Jackson ObjectMapper - serializes ApiError to JSON
     * Same instance used throughout the application (singleton)
     */
    private final ObjectMapper objectMapper;

    /**
     * Handle access denied - user authenticated but lacks permission.
     * 
     * Called by Spring Security when:
     * - User has valid JWT (is authenticated)
     * - But doesn't have required role/authority for the endpoint
     * 
     * @param request The HTTP request that was denied
     * @param response HTTP response to write error to
     * @param accessDeniedException Contains details about why access was denied
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        // Log for security auditing (WARN level - potential security concern)
        // Someone authenticated is trying to access something they shouldn't
        log.warn("Access denied for request to '{}': {}", request.getRequestURI(), accessDeniedException.getMessage());

        /*
         * Build ApiError response matching our standard error format
         * 
         * NOTE: We don't reveal WHY access was denied (security best practice)
         * Don't say "requires ROLE_ADMIN" - that's information disclosure
         * Just say "you don't have permission"
         */
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())  // 403
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())  // "Forbidden"
                .code(AuthErrorCode.ACCESS_DENIED.getCode())  // "AUTH_003"
                .message("You don't have permission to access this resource")
                .path(request.getRequestURI())
                .build();

        // Set response headers and status
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);  // "application/json"
        response.setStatus(HttpStatus.FORBIDDEN.value());  // 403
        
        // Write JSON body directly to response output stream
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
