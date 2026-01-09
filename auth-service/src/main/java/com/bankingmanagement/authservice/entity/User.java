package com.bankingmanagement.authservice.entity;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                                    USER ENTITY                                            ║
║                                                                                           ║
║  BUILD ORDER: STEP 3b of 12 (Second Entity)                                              ║
║  PREVIOUS STEP: Role.java (this entity references it)                                    ║
║  NEXT STEP: RefreshToken.java (references this entity)                                   ║
║                                                                                           ║
║  WHY USER COMES AFTER ROLE:                                                              ║
║  - User has a ManyToMany relationship with Role                                          ║
║  - Java/JPA requires Role class to exist before referencing it                           ║
║  - The @JoinTable creates a "user_roles" junction table                                  ║
║                                                                                           ║
║  WHAT THIS CLASS DOES:                                                                    ║
║  - Defines the "users" database table                                                    ║
║  - Stores user credentials (username, email, password hash)                              ║
║  - Stores profile info (firstName, lastName, phone)                                      ║
║  - Manages account status (enabled, locked, expired)                                     ║
║  - Links to roles via ManyToMany relationship                                            ║
║                                                                                           ║
║  TABLE STRUCTURE:                                                                         ║
║  ┌────────────────────────────────────────────────────────────────┐                      ║
║  │                          users                                  │                      ║
║  ├────────────────────────────────────────────────────────────────┤                      ║
║  │ id                    UUID (PK)                                │                      ║
║  │ username              VARCHAR(50) UNIQUE NOT NULL              │                      ║
║  │ email                 VARCHAR(100) UNIQUE NOT NULL             │                      ║
║  │ password              VARCHAR(255) NOT NULL (BCrypt hash)      │                      ║
║  │ first_name            VARCHAR(50)                              │                      ║
║  │ last_name             VARCHAR(50)                              │                      ║
║  │ phone_number          VARCHAR(20)                              │                      ║
║  │ enabled               BOOLEAN DEFAULT true                     │                      ║
║  │ account_non_expired   BOOLEAN DEFAULT true                     │                      ║
║  │ account_non_locked    BOOLEAN DEFAULT true                     │                      ║
║  │ credentials_non_expired BOOLEAN DEFAULT true                   │                      ║
║  │ customer_id           UUID (FK to customer-service)            │                      ║
║  │ last_login_at         TIMESTAMP                                │                      ║
║  │ failed_login_attempts INT DEFAULT 0                            │                      ║
║  │ lock_time             TIMESTAMP                                │                      ║
║  │ created_at            TIMESTAMP NOT NULL                       │                      ║
║  │ updated_at            TIMESTAMP                                │                      ║
║  └────────────────────────────────────────────────────────────────┘                      ║
║                                                                                           ║
║  JUNCTION TABLE (auto-created by JPA):                                                   ║
║  ┌─────────────────────────────────────┐                                                 ║
║  │           user_roles                 │                                                 ║
║  ├─────────────────────────────────────┤                                                 ║
║  │ user_id    UUID (FK to users)       │                                                 ║
║  │ role_id    UUID (FK to roles)       │                                                 ║
║  └─────────────────────────────────────┘                                                 ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User entity representing authenticated users in the banking system.
 * Stores credentials, profile information, and role assignments.
 * 
 * DESIGN DECISIONS:
 * 
 * 1. WHY THESE BOOLEAN FLAGS (enabled, accountNonLocked, etc.)?
 *    - Spring Security's UserDetails interface requires them
 *    - Allows fine-grained account control:
 *      • enabled=false: Admin disabled the account
 *      • accountNonLocked=false: Too many failed logins
 *      • accountNonExpired=false: Account validity period ended
 *      • credentialsNonExpired=false: Password needs to be changed
 * 
 * 2. WHY STORE PASSWORD AS STRING (not char[])?
 *    - JPA/Hibernate requires String for mapping
 *    - Password is BCrypt hashed (not plaintext)
 *    - BCrypt hash is safe to store as String
 * 
 * 3. WHY FetchType.EAGER FOR ROLES?
 *    - Roles are almost always needed when loading a User
 *    - Avoids LazyInitializationException in security context
 *    - Small data (usually 1-3 roles per user)
 * 
 * 4. WHY Instant FOR TIMESTAMPS (not LocalDateTime)?
 *    - Instant is UTC-based (no timezone confusion)
 *    - Consistent with other services in the project
 *    - Easy to convert to any timezone for display
 */
@Entity
@Table(
    name = "users",
    /*
     * Database Indexes: Speed up common queries
     * - idx_user_email: Fast login by email
     * - idx_user_username: Fast login by username
     * Both are UNIQUE to prevent duplicates
     */
    indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_username", columnList = "username", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Primary Key: UUID for security and distributed systems.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Username: Unique identifier chosen by user.
     * Used for login (alternative to email).
     * 
     * Constraints:
     * - NOT NULL: Required field
     * - UNIQUE: No two users can have same username
     * - max 50 chars: Reasonable limit
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Email: Unique identifier, also used for login.
     * 
     * WHY ALLOW LOGIN WITH BOTH USERNAME AND EMAIL?
     * - Better user experience (people forget usernames)
     * - Email is more memorable
     * - Common pattern in modern applications
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Password: BCrypt hashed password.
     * 
     * NEVER stores plaintext password!
     * Example hash: $2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYF4...
     * 
     * BCrypt includes:
     * - Algorithm version ($2a$)
     * - Cost factor ($12$)
     * - Salt (22 chars)
     * - Hash (31 chars)
     */
    @Column(nullable = false)
    private String password;

    // ========================= PROFILE FIELDS =========================
    
    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    // ========================= ACCOUNT STATUS FLAGS =========================
    /*
     * These flags implement Spring Security's UserDetails contract.
     * All default to TRUE (account is usable).
     * 
     * @Builder.Default: Ensures builder pattern uses these defaults
     */

    /** Is the account enabled? False = admin disabled it */
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    /** Has the account expired? For time-limited accounts */
    @Builder.Default
    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired = true;

    /** Is the account locked? False = too many failed logins */
    @Builder.Default
    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    /** Have credentials expired? For forced password changes */
    @Builder.Default
    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired = true;

    // ========================= RELATIONSHIPS =========================

    /**
     * Many-to-Many relationship with Role entity.
     * 
     * HOW IT WORKS:
     * - Creates a junction table "user_roles"
     * - Each row links one user_id to one role_id
     * - A user can have multiple roles
     * - A role can belong to multiple users
     * 
     * FetchType.EAGER:
     * - Roles are loaded immediately with User
     * - Needed because roles are checked on every request
     * 
     * @JoinTable defines the junction table:
     * - name: Table name
     * - joinColumns: Foreign key to this entity (User)
     * - inverseJoinColumns: Foreign key to other entity (Role)
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Optional link to customer-service.
     * 
     * WHY STORE THIS?
     * - Links auth user to their customer profile
     * - Allows looking up customer details
     * - Not required (admin users might not be customers)
     */
    @Column(name = "customer_id")
    private UUID customerId;

    // ========================= SECURITY TRACKING =========================

    /** When did the user last successfully log in? */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** How many consecutive failed login attempts? */
    @Column(name = "failed_login_attempts")
    @Builder.Default
    private int failedLoginAttempts = 0;

    /** When was the account locked? (null = not locked) */
    @Column(name = "lock_time")
    private Instant lockTime;

    // ========================= AUDIT TIMESTAMPS =========================
    /*
     * Hibernate automatically manages these:
     * - @CreationTimestamp: Set once when entity is first saved
     * - @UpdateTimestamp: Updated every time entity is modified
     */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ========================= HELPER METHODS =========================
    /*
     * These methods provide a clean API for common operations.
     * Better than directly manipulating the Set<Role> from outside.
     */

    /** Add a role to this user */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /** Remove a role from this user */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /** Increment failed login counter (for account locking) */
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    /** Reset failed attempts (after successful login) */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }

    /** Record a successful login */
    public void recordLogin() {
        this.lastLoginAt = Instant.now();
        this.failedLoginAttempts = 0;
    }
}
