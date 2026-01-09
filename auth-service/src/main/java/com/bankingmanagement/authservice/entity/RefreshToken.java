package com.bankingmanagement.authservice.entity;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                              REFRESH TOKEN ENTITY                                         ║
║                                                                                           ║
║  BUILD ORDER: STEP 3c of 12 (Third and Final Entity)                                     ║
║  PREVIOUS STEP: User.java (this entity references it via ManyToOne)                      ║
║  NEXT STEP: DTOs (Step 4) - Now that we have our domain model, define API contracts      ║
║                                                                                           ║
║  WHY REFRESH TOKEN COMES LAST AMONG ENTITIES:                                            ║
║  - It depends on User (has a @ManyToOne relationship)                                    ║
║  - It's a "supporting" entity for the token management feature                           ║
║  - User must exist before we can associate tokens with users                             ║
║                                                                                           ║
║  WHAT THIS CLASS DOES:                                                                    ║
║  - Stores refresh tokens in the database                                                 ║
║  - Links each token to a specific user                                                   ║
║  - Tracks token expiration and revocation status                                         ║
║  - Enables "logout all devices" functionality                                            ║
║  - Supports token rotation (replacedByToken field)                                       ║
║                                                                                           ║
║  WHY STORE REFRESH TOKENS IN DATABASE?                                                   ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  APPROACH 1: Stateless (Token-only)                                                 │ ║
║  │  - Refresh token = long-lived JWT with user info embedded                           │ ║
║  │  - Pro: No database lookup needed                                                   │ ║
║  │  - Con: CANNOT REVOKE a token! If stolen, attacker has access until expiry          │ ║
║  │                                                                                     │ ║
║  │  APPROACH 2: Database-backed (OUR CHOICE)                                           │ ║
║  │  - Refresh token = random string, user info stored in DB                            │ ║
║  │  - Pro: Can revoke anytime (delete from DB)                                         │ ║
║  │  - Pro: Can "logout all devices" (delete all user's tokens)                         │ ║
║  │  - Pro: Can track active sessions                                                   │ ║
║  │  - Con: DB lookup on token refresh (acceptable tradeoff)                            │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  TOKEN FLOW:                                                                              ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  1. User logs in                                                                    │ ║
║  │     → Access Token (JWT, 24h) returned                                             │ ║
║  │     → Refresh Token (random string, 7d) saved to DB and returned                   │ ║
║  │                                                                                     │ ║
║  │  2. Access Token expires after 24h                                                  │ ║
║  │     → Client sends Refresh Token to /api/auth/refresh                              │ ║
║  │     → Server looks up token in DB                                                  │ ║
║  │     → If valid and not expired: issue new Access Token                             │ ║
║  │     → If invalid/expired/revoked: return 401, user must re-login                   │ ║
║  │                                                                                     │ ║
║  │  3. User logs out                                                                   │ ║
║  │     → Refresh Token marked as revoked in DB                                        │ ║
║  │     → Access Token still works until expiry (stateless)                            │ ║
║  │     → For immediate invalidation, use short access token + blacklist               │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  TABLE STRUCTURE:                                                                         ║
║  ┌────────────────────────────────────────────────────────────────┐                      ║
║  │                      refresh_tokens                             │                      ║
║  ├────────────────────────────────────────────────────────────────┤                      ║
║  │ id                 UUID (PK)                                   │                      ║
║  │ token              VARCHAR(255) UNIQUE NOT NULL                │                      ║
║  │ user_id            UUID (FK to users) NOT NULL                 │                      ║
║  │ expires_at         TIMESTAMP NOT NULL                          │                      ║
║  │ revoked            BOOLEAN DEFAULT false                       │                      ║
║  │ revoked_at         TIMESTAMP (when revoked)                    │                      ║
║  │ replaced_by_token  VARCHAR(255) (for token rotation)           │                      ║
║  │ user_agent         VARCHAR(500) (browser/device info)          │                      ║
║  │ ip_address         VARCHAR(50) (IPv4 or IPv6)                  │                      ║
║  │ created_at         TIMESTAMP NOT NULL                          │                      ║
║  └────────────────────────────────────────────────────────────────┘                      ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * RefreshToken entity for managing JWT refresh tokens.
 * Allows users to obtain new access tokens without re-authenticating.
 * 
 * DESIGN DECISIONS:
 * 
 * 1. WHY ManyToOne (not OneToOne)?
 *    - A user can have multiple refresh tokens (one per device/browser)
 *    - Allows logging out of specific devices
 *    - Supports "logout all devices" by revoking all tokens for a user
 * 
 * 2. WHY STORE user_agent AND ip_address?
 *    - Security: Show user their active sessions
 *    - Security: Detect suspicious logins from new locations
 *    - UX: "Your account was accessed from Chrome on Windows"
 * 
 * 3. WHY BOTH 'revoked' FLAG AND 'expires_at'?
 *    - expires_at: Natural expiration after 7 days
 *    - revoked: Manual invalidation (user clicked logout)
 *    - A token is invalid if revoked=true OR expiresAt < now()
 * 
 * 4. WHY 'replaced_by_token' FIELD?
 *    - Supports TOKEN ROTATION security pattern
 *    - When a refresh token is used, it's replaced with a new one
 *    - Old token points to new token for debugging/audit trail
 *    - Helps detect token theft (if old token is reused after rotation)
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        /*
         * Index on 'token' for fast lookup during refresh.
         * Every refresh request needs to find the token by value.
         * Without index: O(n) table scan
         * With index: O(log n) B-tree lookup
         */
        @Index(name = "idx_refresh_token", columnList = "token", unique = true),
        /*
         * Index on 'user_id' for finding all user's tokens.
         * Needed for "logout all devices" functionality.
         */
        @Index(name = "idx_refresh_token_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The actual token value - a random secure string.
     * 
     * Generated using UUID.randomUUID(), looks like:
     * "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     * 
     * This is what the client stores and sends back.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * Reference to the user who owns this token.
     * 
     * @ManyToOne: Many tokens can belong to one user
     * FetchType.LAZY: Don't load User unless needed (performance)
     * @JoinColumn: Creates 'user_id' foreign key column
     * nullable = false: Every token must have a user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * When this token expires (typically 7 days from creation).
     * 
     * After this time, the token is invalid even if not revoked.
     * Forces users to re-authenticate periodically for security.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Whether this token has been manually revoked.
     * 
     * Set to true when:
     * - User logs out
     * - Admin revokes the session
     * - Token is rotated (replaced with new token)
     * - Security concern requires invalidation
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    /**
     * When this token was revoked.
     * 
     * Null if not revoked yet.
     * Useful for audit trail and debugging.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * If this token was rotated, points to the replacement token.
     * 
     * TOKEN ROTATION PATTERN:
     * 1. User sends refresh token "ABC" to get new access token
     * 2. Server creates new refresh token "DEF"
     * 3. Token "ABC" is revoked, replacedByToken = "DEF"
     * 4. User now has token "DEF"
     * 
     * WHY TOKEN ROTATION?
     * - If attacker steals "ABC" and tries to use it:
     *   - If used BEFORE legitimate user: creates "GHI", ABC->GHI
     *   - When legitimate user tries "ABC": sees it's revoked → BREACH DETECTED
     * - Limits damage window of stolen tokens
     */
    @Column(name = "replaced_by_token")
    private String replacedByToken;

    /**
     * Browser/device information from User-Agent header.
     * 
     * Examples:
     * - "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0"
     * - "Mobile App v2.1.0 iOS 17.2"
     * 
     * Used for:
     * - Showing user their active sessions
     * - Security alerts for new devices
     * 
     * VARCHAR(500) because User-Agent strings can be long.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * IP address where the token was created from.
     * 
     * VARCHAR(50) because:
     * - IPv4: max 15 chars (255.255.255.255)
     * - IPv6: max 45 chars (full notation)
     * - Extra space for edge cases
     * 
     * Used for:
     * - Security auditing
     * - Detecting logins from unusual locations
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * When this token was created.
     * 
     * @CreationTimestamp: Hibernate sets this automatically on insert.
     * updatable = false: Cannot be changed after creation.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ========================= HELPER METHODS =========================

    /**
     * Check if this token is expired based on current time.
     * 
     * @return true if expiresAt is in the past
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * Check if this token can be used (not expired and not revoked).
     * 
     * This is the method to call when validating a refresh token:
     * if (!refreshToken.isValid()) {
     *     throw new InvalidTokenException("Token is expired or revoked");
     * }
     * 
     * @return true if the token is still usable
     */
    public boolean isValid() {
        return !this.revoked && !isExpired();
    }

    /**
     * Revoke this token (make it invalid).
     * 
     * Called when:
     * - User logs out
     * - Admin terminates session
     * - Token rotation occurs
     * - Security incident
     * 
     * Also records WHEN the revocation happened for audit purposes.
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }
}
