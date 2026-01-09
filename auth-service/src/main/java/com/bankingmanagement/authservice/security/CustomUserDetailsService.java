package com.bankingmanagement.authservice.security;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                         CUSTOM USER DETAILS SERVICE                                       ║
║                                                                                           ║
║  BUILD ORDER: STEP 7b of 12 (Second Security Component)                                  ║
║  PREVIOUS STEP: UserPrincipal (this service creates UserPrincipal objects)               ║
║  NEXT STEP: JwtTokenProvider                                                             ║
║                                                                                           ║
║  WHAT IS UserDetailsService? (Spring Security Interface)                                 ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Spring Security needs a way to LOAD users from your data store.                    │ ║
║  │  UserDetailsService is the interface for this:                                      │ ║
║  │                                                                                     │ ║
║  │  interface UserDetailsService {                                                     │ ║
║  │      UserDetails loadUserByUsername(String username);                               │ ║
║  │  }                                                                                  │ ║
║  │                                                                                     │ ║
║  │  We implement this to tell Spring Security HOW to find our users.                   │ ║
║  │  Spring Security calls this during authentication.                                  │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  AUTHENTICATION FLOW:                                                                    ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  1. User submits login form: { username: "john", password: "secret" }               │ ║
║  │                                                                                     │ ║
║  │  2. AuthenticationManager calls:                                                    │ ║
║  │     userDetailsService.loadUserByUsername("john")                                   │ ║
║  │                                                                                     │ ║
║  │  3. Our service:                                                                    │ ║
║  │     - Queries database: userRepository.findByUsernameOrEmail("john", "john")        │ ║
║  │     - Creates UserPrincipal from User entity                                        │ ║
║  │     - Returns UserPrincipal (contains password hash)                                │ ║
║  │                                                                                     │ ║
║  │  4. Spring Security:                                                                │ ║
║  │     - Compares provided "secret" with stored hash                                   │ ║
║  │     - If match: Authentication success                                              │ ║
║  │     - If mismatch: BadCredentialsException                                          │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY loadUserById() METHOD?                                                              ║
║  - loadUserByUsername() is for initial login                                             ║
║  - loadUserById() is for JWT token validation                                            ║
║  - JWT contains user ID (not username) for efficiency                                    ║
║  - When validating JWT, we look up user by ID to refresh permissions                     ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.entity.User;
import com.bankingmanagement.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Custom UserDetailsService implementation.
 * Loads user details from the database for authentication.
 * 
 * This is the BRIDGE between Spring Security and our UserRepository.
 * Spring Security doesn't know about JPA - this service translates.
 * 
 * @Service marks this as a Spring-managed service bean
 * @RequiredArgsConstructor injects UserRepository via constructor
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username or email for authentication.
     * 
     * Called by Spring Security's AuthenticationManager during login.
     * The parameter is whatever the user typed in the "username" field.
     * 
     * WHY ACCEPT BOTH USERNAME AND EMAIL?
     * - Better user experience (users forget usernames)
     * - We pass the same value to both parameters of findByUsernameOrEmail
     * - Database checks: WHERE username = ? OR email = ?
     * 
     * @param usernameOrEmail Whatever the user entered (could be either)
     * @return UserDetails containing user info and password hash
     * @throws UsernameNotFoundException if user not found (Spring Security catches this)
     */
    @Override
    @Transactional(readOnly = true)  // Read-only transaction (optimization)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user by username or email: {}", usernameOrEmail);

        // Try to find by username OR email (single query)
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmail
                ));

        // Convert to Spring Security's UserDetails
        return UserPrincipal.create(user);
    }

    /**
     * Load user by ID (used for JWT token validation).
     * 
     * NOT part of UserDetailsService interface - this is OUR addition.
     * 
     * WHY NEEDED?
     * - JWT token contains user ID (in 'sub' claim)
     * - When validating JWT, we need to load fresh user data
     * - This lets us check if user still exists, is still enabled, etc.
     * 
     * CALLED BY: JwtAuthenticationFilter.doFilterInternal()
     * 
     * @param userId UUID from JWT token's subject claim
     * @return UserDetails with current user data
     * @throws UsernameNotFoundException if user no longer exists
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID userId) {
        log.debug("Loading user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with ID: " + userId
                ));

        return UserPrincipal.create(user);
    }
}
