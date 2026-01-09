package com.bankingmanagement.authservice.config;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                           SPRING SECURITY CONFIGURATION                                   ║
║                                                                                           ║
║  BUILD ORDER: STEP 9a of 12 (Configuration - Security Setup)                             ║
║  PREVIOUS STEP: AuthServiceImpl (service layer complete, now wire up security)           ║
║  NEXT STEP: DataInitializer (seed database with roles)                                   ║
║                                                                                           ║
║  THIS IS WHERE ALL SECURITY COMPONENTS COME TOGETHER                                     ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  SPRING SECURITY ARCHITECTURE:                                                           ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  HTTP Request ───────────────────────────────────────────────────────────────────>  │ ║
║  │       │                                                                             │ ║
║  │       ▼                                                                             │ ║
║  │  ┌─────────────────────────────────────────────────────────────────────────────┐   │ ║
║  │  │                    FILTER CHAIN (ordered)                                   │   │ ║
║  │  │                                                                             │   │ ║
║  │  │  ┌─────────────────┐                                                        │   │ ║
║  │  │  │ CORS Filter     │ ← Handles cross-origin requests                        │   │ ║
║  │  │  └────────┬────────┘                                                        │   │ ║
║  │  │           ▼                                                                 │   │ ║
║  │  │  ┌─────────────────┐                                                        │   │ ║
║  │  │  │ JwtAuth Filter  │ ← OUR FILTER - validates JWT, sets SecurityContext     │   │ ║
║  │  │  └────────┬────────┘                                                        │   │ ║
║  │  │           ▼                                                                 │   │ ║
║  │  │  ┌─────────────────┐                                                        │   │ ║
║  │  │  │ UsernamePassword│ ← (We skip this - already handled by JWT filter)       │   │ ║
║  │  │  │ AuthFilter      │                                                        │   │ ║
║  │  │  └────────┬────────┘                                                        │   │ ║
║  │  │           ▼                                                                 │   │ ║
║  │  │  ┌─────────────────┐                                                        │   │ ║
║  │  │  │ ExceptionTransl.│ ← Handles authentication exceptions                    │   │ ║
║  │  │  │ Filter          │                                                        │   │ ║
║  │  │  └────────┬────────┘                                                        │   │ ║
║  │  │           ▼                                                                 │   │ ║
║  │  │  ┌─────────────────┐                                                        │   │ ║
║  │  │  │ FilterSecurity  │ ← Checks authorization rules (permitAll, hasRole)      │   │ ║
║  │  │  │ Interceptor     │                                                        │   │ ║
║  │  │  └─────────────────┘                                                        │   │ ║
║  │  │                                                                             │   │ ║
║  │  └─────────────────────────────────────────────────────────────────────────────┘   │ ║
║  │       │                                                                             │ ║
║  │       ▼                                                                             │ ║
║  │  ┌─────────────────┐                                                                │ ║
║  │  │   Controller    │                                                                │ ║
║  │  └─────────────────┘                                                                │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  KEY CONFIGURATION DECISIONS:                                                            ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  1. CSRF DISABLED - Why?                                                            │ ║
║  │     - CSRF attacks use browser cookies                                              │ ║
║  │     - Our JWT is in Authorization header, not cookies                               │ ║
║  │     - Stateless APIs don't need CSRF protection                                     │ ║
║  │                                                                                     │ ║
║  │  2. STATELESS SESSIONS - Why?                                                       │ ║
║  │     - JWT contains all necessary auth data                                          │ ║
║  │     - No server-side session storage needed                                         │ ║
║  │     - Each request is self-contained                                                │ ║
║  │     - Better for scaling (no session affinity required)                             │ ║
║  │                                                                                     │ ║
║  │  3. FORM LOGIN DISABLED - Why?                                                      │ ║
║  │     - This is a REST API, not a web app                                             │ ║
║  │     - No login form, no redirects                                                   │ ║
║  │     - Login via POST /api/auth/login with JSON body                                 │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  AUTHENTICATION vs AUTHORIZATION:                                                        ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  AUTHENTICATION (WHO are you?):                                                     │ ║
║  │  - Handled by JwtAuthenticationFilter                                               │ ║
║  │  - Validates JWT token                                                              │ ║
║  │  - Sets Authentication in SecurityContext                                           │ ║
║  │                                                                                     │ ║
║  │  AUTHORIZATION (WHAT can you do?):                                                  │ ║
║  │  - URL-based: .requestMatchers("/api/admin/**").hasRole("ADMIN")                    │ ║
║  │  - Method-based: @PreAuthorize("hasRole('ADMIN')") on controllers                   │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.security.CustomUserDetailsService;
import com.bankingmanagement.authservice.security.JwtAccessDeniedHandler;
import com.bankingmanagement.authservice.security.JwtAuthenticationEntryPoint;
import com.bankingmanagement.authservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for JWT authentication.
 * 
 * ANNOTATIONS:
 * @Configuration - Marks this as Spring configuration class
 * @EnableWebSecurity - Enables Spring Security's web security support
 * @EnableMethodSecurity - Enables @PreAuthorize, @PostAuthorize annotations
 * @RequiredArgsConstructor - Injects final fields via constructor
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // Enable @PreAuthorize("hasRole('ADMIN')") etc.
@RequiredArgsConstructor
public class SecurityConfig {

    /*
     * DEPENDENCIES - All our security components
     * Injected via constructor (RequiredArgsConstructor)
     */
    private final CustomUserDetailsService userDetailsService;      // Loads users from DB
    private final JwtAuthenticationFilter jwtAuthenticationFilter;  // Validates JWT
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;  // Handles 401
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;    // Handles 403

    /**
     * Public endpoints that don't require authentication.
     * 
     * These endpoints are accessible without a JWT token:
     * - /register - New user signup
     * - /login - Get tokens
     * - /refresh - Exchange refresh token for new access token
     * - /validate - Check if token is valid (for other services)
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/validate"
    };

    /**
     * Swagger/OpenAPI endpoints for API documentation.
     * Public for development convenience.
     */
    private static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**"
    };

    /**
     * Actuator endpoints for health checks and monitoring.
     * Consider restricting in production!
     */
    private static final String[] ACTUATOR_ENDPOINTS = {
            "/actuator/**"
    };

    /**
     * Configure the security filter chain.
     * 
     * This is the MAIN security configuration method.
     * Defines which endpoints are public, which require auth, and how auth works.
     * 
     * @Bean makes this a Spring-managed singleton
     * @param http HttpSecurity builder (fluent API)
     * @return Configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                /*
                 * CSRF (Cross-Site Request Forgery) - DISABLED
                 * 
                 * Why disable?
                 * - CSRF attacks exploit cookies
                 * - Our JWT is sent via Authorization header, not cookies
                 * - Stateless APIs don't need CSRF protection
                 * 
                 * AbstractHttpConfigurer::disable is method reference for lambda: csrf -> csrf.disable()
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                 * Form Login & HTTP Basic - DISABLED
                 * 
                 * Why disable?
                 * - This is a REST API, not a traditional web app
                 * - No login form, no browser popups
                 * - Authentication via POST /api/auth/login with JSON body
                 */
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                /*
                 * Exception Handling Configuration
                 * 
                 * AuthenticationEntryPoint: Handles unauthenticated requests (401)
                 * AccessDeniedHandler: Handles unauthorized requests (403)
                 * 
                 * Our custom handlers return JSON instead of redirecting to login page
                 */
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 401 handler
                        .accessDeniedHandler(jwtAccessDeniedHandler)            // 403 handler
                )

                /*
                 * Session Management - STATELESS
                 * 
                 * SessionCreationPolicy.STATELESS means:
                 * - Spring Security won't create HttpSession
                 * - Won't use HttpSession for authentication
                 * - Each request must contain complete auth info (JWT)
                 * 
                 * Benefits:
                 * - Better scalability (no session affinity needed)
                 * - Works well with microservices
                 * - Each request is independent
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                /*
                 * Authorization Rules - WHO CAN ACCESS WHAT
                 * 
                 * Order matters! First match wins.
                 * More specific rules should come before general rules.
                 */
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Swagger documentation - public for development
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()

                        // Actuator endpoints - health checks, metrics
                        .requestMatchers(ACTUATOR_ENDPOINTS).permitAll()

                        // H2 Console - local development only (should be disabled in production)
                        .requestMatchers("/h2-console/**").permitAll()

                        // Admin-only endpoints - requires ROLE_ADMIN
                        // Note: hasRole("ADMIN") automatically adds "ROLE_" prefix
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // All other requests require authentication
                        // User must have valid JWT to access
                        .anyRequest().authenticated()
                )

                /*
                 * Add JWT Filter BEFORE UsernamePasswordAuthenticationFilter
                 * 
                 * Why "before"?
                 * - Our JWT filter extracts user from token and sets SecurityContext
                 * - UsernamePasswordAuthenticationFilter expects form login
                 * - We want JWT checked first, skip username/password filter
                 */
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                /*
                 * Headers Configuration - Allow H2 Console
                 * 
                 * H2 Console uses iframes, which are blocked by default
                 * frameOptions.sameOrigin() allows iframes from same origin
                 * 
                 * WARNING: This is for local development only!
                 */
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    // ==================== BEAN DEFINITIONS ====================

    /**
     * Password Encoder - BCrypt with strength 12.
     * 
     * BCrypt:
     * - Adaptive hashing (can increase iterations as hardware improves)
     * - Built-in salt (different hash for same password)
     * - Strength 12 = 2^12 = 4096 iterations
     * 
     * Higher strength = more secure but slower.
     * 10-12 is good balance for most applications.
     * 
     * Usage: passwordEncoder.encode("password") / passwordEncoder.matches("password", hash)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication Provider - Connects UserDetailsService with PasswordEncoder.
     * 
     * DaoAuthenticationProvider:
     * - "DAO" = Data Access Object
     * - Uses UserDetailsService to load user from database
     * - Uses PasswordEncoder to verify password
     * 
     * Flow when AuthenticationManager.authenticate() is called:
     * 1. Load UserDetails via userDetailsService.loadUserByUsername()
     * 2. Compare passwords using passwordEncoder.matches()
     * 3. Return authenticated Authentication or throw exception
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);  // How to load users
        provider.setPasswordEncoder(passwordEncoder());       // How to verify passwords
        return provider;
    }

    /**
     * Authentication Manager - Main entry point for authentication.
     * 
     * Injected into AuthServiceImpl to authenticate login requests.
     * 
     * Usage: authenticationManager.authenticate(UsernamePasswordAuthenticationToken)
     * 
     * Spring Boot auto-configures AuthenticationManager with our AuthenticationProvider.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
