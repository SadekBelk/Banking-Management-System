package com.bankingmanagement.authservice.security;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                              USER PRINCIPAL (UserDetails)                                 ║
║                                                                                           ║
║  BUILD ORDER: STEP 7a of 12 (First Security Component)                                   ║
║  PREVIOUS STEP: Exception classes (Step 6)                                               ║
║  NEXT STEP: CustomUserDetailsService                                                     ║
║                                                                                           ║
║  WHY SECURITY COMPONENTS COME AFTER EXCEPTIONS:                                          ║
║  - Security components throw exceptions (InvalidTokenException, etc.)                    ║
║  - Need exception classes defined before we can use them                                 ║
║  - Security is a cross-cutting concern that uses all lower layers                        ║
║                                                                                           ║
║  WHAT IS UserDetails? (Spring Security Interface)                                        ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Spring Security doesn't know about your User entity.                               │ ║
║  │  It only understands UserDetails interface.                                         │ ║
║  │                                                                                     │ ║
║  │  UserDetails provides:                                                              │ ║
║  │  - getUsername() - Identity for authentication                                      │ ║
║  │  - getPassword() - Password hash for verification                                   │ ║
║  │  - getAuthorities() - Roles/permissions for authorization                           │ ║
║  │  - isEnabled(), isAccountNonLocked(), etc. - Account status checks                  │ ║
║  │                                                                                     │ ║
║  │  UserPrincipal is our ADAPTER that makes User work with Spring Security             │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY NOT MAKE User IMPLEMENT UserDetails DIRECTLY?                                       ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Option A: User implements UserDetails (simpler but BAD)                            │ ║
║  │  - Mixes domain model with security concerns                                        │ ║
║  │  - User entity leaks into security context                                          │ ║
║  │  - Hard to unit test (JPA entity in security tests)                                 │ ║
║  │                                                                                     │ ║
║  │  Option B: Separate UserPrincipal (our approach - GOOD)                             │ ║
║  │  - Clean separation of concerns                                                     │ ║
║  │  - User entity stays pure (JPA only)                                                │ ║
║  │  - UserPrincipal is lightweight (no JPA baggage)                                    │ ║
║  │  - Easy to test security components independently                                   │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom UserDetails implementation for Spring Security.
 * Wraps the User entity and provides security-related information.
 * 
 * This class is:
 * - Created from User entity via create() factory method
 * - Stored in SecurityContext after successful authentication
 * - Used by Spring Security for authorization checks
 * - Used by our code to get current user info
 * 
 * LIFECYCLE:
 * 1. User logs in → CustomUserDetailsService.loadUserByUsername()
 * 2. Service loads User from DB → UserPrincipal.create(user)
 * 3. Spring Security verifies password
 * 4. UserPrincipal stored in SecurityContextHolder
 * 5. Controller accesses via @AuthenticationPrincipal UserPrincipal principal
 */
@Getter      // Lombok: generates getters (required by UserDetails)
@Builder     // Lombok: enables UserPrincipal.builder()...build()
@AllArgsConstructor  // Lombok: constructor for all fields (used by builder)
public class UserPrincipal implements UserDetails {

    // ========================= OUR CUSTOM FIELDS =========================
    /*
     * These fields hold user data extracted from the User entity.
     * We keep id and email because they're useful for our business logic.
     */

    /** User's UUID - needed for database operations */
    private final UUID id;

    /** Username for display and identification */
    private final String username;

    /** Email - useful for notifications, display */
    private final String email;

    /** BCrypt password hash - Spring Security compares against provided password */
    private final String password;

    /** Profile info - useful for display in UI */
    private final String firstName;
    private final String lastName;

    // ========================= SPRING SECURITY FLAGS =========================
    /*
     * These map directly to User entity boolean fields.
     * Spring Security checks these BEFORE allowing authentication.
     */

    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;

    /** Roles/permissions - used for @PreAuthorize, hasRole() checks */
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Factory method: Create UserPrincipal from User entity.
     * 
     * This is the BRIDGE between our domain model (User) and Spring Security.
     * Called by CustomUserDetailsService when loading user for authentication.
     * 
     * AUTHORITY CONVERSION:
     * User.roles (Set<Role>) → authorities (Collection<GrantedAuthority>)
     * Role.name (enum ROLE_USER) → SimpleGrantedAuthority("ROLE_USER")
     * 
     * @param user The User entity from database
     * @return UserPrincipal for Spring Security
     */
    public static UserPrincipal create(User user) {
        // Convert Role entities to Spring Security GrantedAuthority objects
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .authorities(authorities)
                .build();
    }

    // ========================= UserDetails INTERFACE METHODS =========================
    /*
     * These methods are required by Spring Security's UserDetails interface.
     * Spring Security calls these during authentication and authorization.
     */

    /**
     * Returns the authorities (roles) granted to the user.
     * Used by: @PreAuthorize("hasRole('ADMIN')"), hasAuthority(), etc.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Returns the password hash for authentication.
     * Spring Security compares this against the provided password.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the username for identification.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Is the account still valid (not expired)?
     * If false: Spring Security throws AccountExpiredException
     */
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    /**
     * Is the account unlocked?
     * If false: Spring Security throws LockedException
     */
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    /**
     * Are the credentials (password) still valid?
     * If false: Spring Security throws CredentialsExpiredException
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    /**
     * Is the account enabled (not disabled by admin)?
     * If false: Spring Security throws DisabledException
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}