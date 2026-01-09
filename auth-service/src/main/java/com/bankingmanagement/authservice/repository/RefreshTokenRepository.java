package com.bankingmanagement.authservice.repository;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                         REFRESH TOKEN REPOSITORY                                          ║
║                                                                                           ║
║  BUILD ORDER: STEP 5c of 12 (Third and Final Repository)                                 ║
║  PREVIOUS STEP: UserRepository                                                           ║
║  NEXT STEP: Exception classes (Step 6)                                                   ║
║                                                                                           ║
║  WHAT THIS REPOSITORY DOES:                                                              ║
║  - Manages refresh token lifecycle                                                       ║
║  - Token lookup for refresh flow                                                         ║
║  - Bulk operations for logout/revocation                                                 ║
║  - Cleanup of expired tokens                                                             ║
║                                                                                           ║
║  TOKEN LIFECYCLE:                                                                        ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  CREATE: User logs in → save(new RefreshToken)                                      │ ║
║  │      ↓                                                                              │ ║
║  │  USE: Client refreshes → findByToken()                                              │ ║
║  │      ↓                                                                              │ ║
║  │  REVOKE: User logs out → token.revoke() or delete()                                 │ ║
║  │      ↓                                                                              │ ║
║  │  CLEANUP: Scheduled job → deleteExpiredAndRevokedTokens()                           │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.entity.RefreshToken;
import com.bankingmanagement.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity operations.
 * 
 * Handles the database-backed refresh token management.
 * This is what makes token revocation possible (unlike stateless JWT-only approach).
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token by its token string value.
     * 
     * THE CORE LOOKUP: Called on every token refresh request.
     * Client sends: { "refreshToken": "abc-123-xyz" }
     * Server does: refreshTokenRepository.findByToken("abc-123-xyz")
     * 
     * Returns Optional because:
     * - Token might not exist (client error or tampering)
     * - Token was deleted (cleanup job)
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all active (non-revoked) tokens for a user.
     * 
     * Used for:
     * - Showing user their active sessions
     * - Limiting concurrent sessions (if max sessions enforced)
     * 
     * Method name breakdown:
     * findBy         - Find entities where...
     * User           - user field equals given user
     * And            - AND
     * RevokedFalse   - revoked field equals false
     */
    List<RefreshToken> findByUserAndRevokedFalse(User user);

    /**
     * Revoke ALL tokens for a user ("logout all devices").
     * 
     * WHY BULK UPDATE INSTEAD OF LOADING AND SAVING EACH?
     * - User might have many tokens (one per device/browser)
     * - Single SQL statement is much faster
     * - No risk of partial updates
     * 
     * WHEN USED:
     * - User clicks "Logout from all devices"
     * - Security incident (force re-auth everywhere)
     * - Admin action (disable user sessions)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllUserTokens(@Param("user") User user, @Param("revokedAt") Instant revokedAt);

    /**
     * Delete expired and revoked tokens (database cleanup).
     * 
     * WHY DELETE INSTEAD OF JUST MARKING REVOKED?
     * - Revoked tokens are useless (can't be un-revoked)
     * - Expired tokens are useless (permanently invalid)
     * - Reduces database size and improves query performance
     * 
     * WHEN TO CALL:
     * - Scheduled job (e.g., daily at 3 AM)
     * - On-demand cleanup triggered by admin
     * 
     * RETURNS: Number of tokens deleted (for logging/monitoring)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    int deleteExpiredAndRevokedTokens(@Param("now") Instant now);

    /**
     * Count active tokens for a user.
     * 
     * Used for:
     * - Enforcing max concurrent sessions per user
     * - Metrics/monitoring
     * 
     * Active = not revoked AND not expired (expiresAt > now)
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokensByUser(@Param("user") User user, @Param("now") Instant now);
}
