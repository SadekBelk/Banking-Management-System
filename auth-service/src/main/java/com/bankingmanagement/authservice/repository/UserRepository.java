package com.bankingmanagement.authservice.repository;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                              USER REPOSITORY                                              ║
║                                                                                           ║
║  BUILD ORDER: STEP 5b of 12 (Second Repository)                                          ║
║  PREVIOUS STEP: RoleRepository                                                           ║
║  NEXT STEP: RefreshTokenRepository                                                       ║
║                                                                                           ║
║  WHAT THIS REPOSITORY DOES:                                                              ║
║  - All database operations for the User entity                                           ║
║  - Login lookup by username/email                                                        ║
║  - Registration checks (username/email exists)                                           ║
║  - Security operations (lock, unlock, track login attempts)                              ║
║                                                                                           ║
║  METHOD NAMING CONVENTIONS:                                                              ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  SPRING DATA JPA QUERY DERIVATION:                                                   │ ║
║  │                                                                                     │ ║
║  │  Method Name           →  Generated SQL                                            │ ║
║  │  ──────────────────────────────────────────────────────────────────────────  │ ║
║  │  findByUsername         →  SELECT * FROM users WHERE username = ?                   │ ║
║  │  findByEmail            →  SELECT * FROM users WHERE email = ?                      │ ║
║  │  findByUsernameOrEmail  →  SELECT * FROM users WHERE username = ? OR email = ?     │ ║
║  │  existsByUsername       →  SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)   │ ║
║  │  existsByEmail          →  SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)      │ ║
║  │                                                                                     │ ║
║  │  Keywords: find, exists, count, delete, By, And, Or, Is, Equals, etc.               │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY @Modifying QUERIES?                                                                 ║
║  - @Modifying tells Spring this query CHANGES data (not just reads)                      ║
║  - Required for UPDATE/DELETE queries                                                   ║
║  - Must be used with @Transactional in the service layer                                ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 * 
 * The most heavily-used repository in auth-service.
 * Handles all user-related database operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ========================= LOOKUP METHODS =========================
    /*
     * These methods find users for authentication.
     * Spring Data JPA generates the SQL automatically based on method names.
     */

    /**
     * Find user by exact username match.
     * Used when login input doesn't contain '@' (assumed to be username).
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by exact email match.
     * Used when login input contains '@' (assumed to be email).
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username OR email (single query, more efficient).
     * 
     * HOW IT'S USED:
     * // Pass the same value to both parameters
     * userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
     * 
     * GENERATED SQL:
     * SELECT * FROM users WHERE username = ? OR email = ?
     * 
     * WHY NOT TWO SEPARATE QUERIES?
     * - More efficient: One database round-trip instead of two
     * - Simpler code: Don't need to check for '@' symbol
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    // ========================= EXISTENCE CHECKS =========================
    /*
     * These methods check if a user exists WITHOUT loading the full entity.
     * More efficient than findBy* when you just need true/false.
     */

    /**
     * Check if username is already taken.
     * Used during registration to prevent duplicate usernames.
     */
    boolean existsByUsername(String username);

    /**
     * Check if email is already registered.
     * Used during registration to prevent duplicate emails.
     */
    boolean existsByEmail(String email);

    // ========================= SECURITY OPERATIONS =========================
    /*
     * These use @Query for custom JPQL (Java Persistence Query Language).
     * More efficient than load-modify-save pattern for single-field updates.
     * 
     * @Modifying = This query changes data (required for UPDATE/DELETE)
     * @Query = Custom JPQL query (not derived from method name)
     * @Param = Binds method parameter to query parameter
     */

    /**
     * Record successful login: update timestamp and reset failed attempts.
     * 
     * WHY USE @Query INSTEAD OF save()?
     * - Only updates 2 fields, not the entire entity
     * - Single SQL statement (more efficient)
     * - Avoids race conditions with concurrent requests
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void updateLoginSuccess(@Param("userId") UUID userId, @Param("loginTime") Instant loginTime);

    /**
     * Increment failed login counter (for brute force protection).
     * 
     * Called when: Password verification fails
     * After N failures: Account gets locked (see lockUser)
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedAttempts(@Param("userId") UUID userId);

    /**
     * Lock a user account (too many failed logins).
     * 
     * Sets accountNonLocked = false, which makes Spring Security reject logins.
     * lockTime is recorded for potential auto-unlock after cooldown period.
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = false, u.lockTime = :lockTime WHERE u.id = :userId")
    void lockUser(@Param("userId") UUID userId, @Param("lockTime") Instant lockTime);

    /**
     * Unlock a user account (admin action or after cooldown).
     * 
     * Resets all lock-related fields to allow login again.
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = true, u.lockTime = null, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void unlockUser(@Param("userId") UUID userId);

    /**
     * Update user's password hash.
     * 
     * NOTE: The password parameter should ALREADY be BCrypt hashed!
     * Never store plaintext passwords in the database.
     */
    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :userId")
    void updatePassword(@Param("userId") UUID userId, @Param("password") String password);
}
