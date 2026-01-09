package com.bankingmanagement.authservice.security;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                            JWT TOKEN PROVIDER                                             ║
║                                                                                           ║
║  BUILD ORDER: STEP 7c of 12 (Third Security Component - CORE JWT LOGIC)                  ║
║  PREVIOUS STEP: CustomUserDetailsService (loads users for token generation)              ║
║  NEXT STEP: JwtAuthenticationFilter (uses this to validate tokens)                       ║
║                                                                                           ║
║  THIS IS THE HEART OF JWT AUTHENTICATION                                                 ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  WHAT IS JWT? (JSON Web Token)                                                           ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  JWT is a compact, URL-safe way to represent claims between two parties.            │ ║
║  │                                                                                     │ ║
║  │  Structure:  HEADER.PAYLOAD.SIGNATURE                                               │ ║
║  │                                                                                     │ ║
║  │  Example:    eyJhbGciOiJIUzI1NiJ9                    <- Header (base64)             │ ║
║  │              .eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI... <- Payload (base64)            │ ║
║  │              .SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV...  <- Signature                   │ ║
║  │                                                                                     │ ║
║  │  ┌────────────────────────────────────────────────────────────────────┐             │ ║
║  │  │ HEADER (Algorithm & Type):                                         │             │ ║
║  │  │ {                                                                  │             │ ║
║  │  │   "alg": "HS256",  // HMAC SHA-256 algorithm                       │             │ ║
║  │  │   "typ": "JWT"     // Token type                                   │             │ ║
║  │  │ }                                                                  │             │ ║
║  │  └────────────────────────────────────────────────────────────────────┘             │ ║
║  │                                                                                     │ ║
║  │  ┌────────────────────────────────────────────────────────────────────┐             │ ║
║  │  │ PAYLOAD (Claims - data we store):                                  │             │ ║
║  │  │ {                                                                  │             │ ║
║  │  │   "jti": "unique-id",           // JWT ID (prevents replay)        │             │ ║
║  │  │   "sub": "user-uuid",           // Subject (user ID)               │             │ ║
║  │  │   "iss": "auth-service",        // Issuer                          │             │ ║
║  │  │   "iat": 1735000000,            // Issued at (timestamp)           │             │ ║
║  │  │   "exp": 1735086400,            // Expiration (timestamp)          │             │ ║
║  │  │   "username": "john",           // Custom claim                    │             │ ║
║  │  │   "email": "john@example.com",  // Custom claim                    │             │ ║
║  │  │   "roles": "ROLE_USER,ROLE_CUSTOMER"  // Custom claim              │             │ ║
║  │  │ }                                                                  │             │ ║
║  │  └────────────────────────────────────────────────────────────────────┘             │ ║
║  │                                                                                     │ ║
║  │  ┌────────────────────────────────────────────────────────────────────┐             │ ║
║  │  │ SIGNATURE (Ensures integrity):                                     │             │ ║
║  │  │                                                                    │             │ ║
║  │  │ HMACSHA256(base64(header) + "." + base64(payload), secret)         │             │ ║
║  │  │                                                                    │             │ ║
║  │  │ If anyone modifies payload, signature won't match!                 │             │ ║
║  │  └────────────────────────────────────────────────────────────────────┘             │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY USE JWT INSTEAD OF SESSIONS?                                                        ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  SESSIONS (Traditional):           JWT (Stateless):                                 │ ║
║  │  ┌─────────────────────┐           ┌─────────────────────┐                          │ ║
║  │  │ Server stores state │           │ Token contains data │                          │ ║
║  │  │ Session ID = "abc"  │           │ No server state     │                          │ ║
║  │  │ -> Lookup in Redis  │           │ -> Self-contained   │                          │ ║
║  │  └─────────────────────┘           └─────────────────────┘                          │ ║
║  │                                                                                     │ ║
║  │  Microservices benefit: Each service can validate JWT independently                │ ║
║  │  No shared session store needed between account-service, payment-service, etc.     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  ACCESS TOKEN vs REFRESH TOKEN:                                                          ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  ACCESS TOKEN (JWT):                 REFRESH TOKEN (Random String):                 │ ║
║  │  - Short-lived (24 hours)            - Long-lived (7 days)                          │ ║
║  │  - Sent with every request           - Only sent to /refresh endpoint              │ ║
║  │  - Contains user data                - Just a random identifier                     │ ║
║  │  - Stateless validation              - Stored in database                           │ ║
║  │  - If stolen: limited damage         - Can be revoked                               │ ║
║  │                                                                                     │ ║
║  │  Flow:                                                                              │ ║
║  │  1. Login -> Get both tokens                                                        │ ║
║  │  2. Use access token for API calls                                                  │ ║
║  │  3. When access token expires -> Use refresh token to get new access token          │ ║
║  │  4. When refresh token expires -> User must login again                             │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  SECURITY: SECRET KEY                                                                    ║
║  - Must be at least 256 bits (32 bytes) for HS256                                        ║
║  - Stored as Base64-encoded string in application.yml                                    ║
║  - NEVER commit real secret to version control                                           ║
║  - Use environment variable JWT_SECRET in production                                     ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Token Provider - handles JWT token generation and validation.
 *
 * Responsibilities:
 * - Generate access tokens with user claims
 * - Generate refresh tokens (random strings stored in DB)
 * - Validate and parse JWT tokens
 * - Extract user information from tokens
 * 
 * @Component makes this a Spring-managed singleton bean
 * Other components inject this to generate/validate tokens
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /*
     * SECRET KEY - Used to sign and verify JWTs
     * 
     * Created from Base64-decoded secret string from application.yml
     * Keys.hmacShaKeyFor() ensures key is proper length for HS256
     */
    private final SecretKey secretKey;
    
    /*
     * Token expiration times (in milliseconds)
     * Loaded from application.yml:
     * - jwt.expiration: 86400000 (24 hours)
     * - jwt.refresh-expiration: 604800000 (7 days)
     */
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    
    /*
     * Issuer claim - identifies who created the token
     * Set to "digital-banking-auth-service" in application.yml
     * Other services can verify tokens came from legitimate auth service
     */
    private final String issuer;

    /**
     * Constructor - Spring injects values from application.yml
     * 
     * @Value annotation pulls values from configuration:
     * - jwt.secret -> 256-bit Base64 encoded secret
     * - jwt.expiration -> access token lifetime in ms
     * - jwt.refresh-expiration -> refresh token lifetime in ms
     * - jwt.issuer -> token issuer identifier
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration,
            @Value("${jwt.issuer}") String issuer
    ) {
        // Decode Base64 secret and create HMAC-SHA key
        // Keys.hmacShaKeyFor() validates key length (256+ bits for HS256)
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.issuer = issuer;
    }

    // ==================== TOKEN GENERATION ====================

    /**
     * Generate JWT access token from Authentication object.
     * 
     * CALLED BY: AuthServiceImpl.login() after successful authentication
     * 
     * @param authentication Contains UserPrincipal in getPrincipal()
     * @return JWT string (header.payload.signature)
     */
    public String generateAccessToken(Authentication authentication) {
        // Cast Principal to our UserPrincipal (set during authentication)
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal);
    }

    /**
     * Generate JWT access token from UserPrincipal.
     * 
     * This is the CORE token generation method.
     * Creates a JWT with:
     * - Standard claims (jti, sub, iss, iat, exp)
     * - Custom claims (username, email, roles)
     * 
     * @param userPrincipal Contains user data to embed in token
     * @return Compact JWT string ready to send to client
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpiration);

        // Convert authorities to comma-separated string
        // e.g., "ROLE_USER,ROLE_CUSTOMER"
        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        /*
         * Build JWT using JJWT library (jjwt-api)
         * 
         * Standard Claims:
         * - id (jti): Unique token ID (prevents replay attacks)
         * - subject (sub): User ID (primary identifier)
         * - issuer (iss): Who created this token
         * - issuedAt (iat): When token was created
         * - expiration (exp): When token expires
         * 
         * Custom Claims:
         * - username: For display/logging
         * - email: For display/notifications
         * - roles: For authorization decisions
         * 
         * signWith(): Signs with our secret key using HS256 algorithm
         * compact(): Serializes to string (header.payload.signature)
         */
        return Jwts.builder()
                .id(UUID.randomUUID().toString())               // jti - unique token ID
                .subject(userPrincipal.getId().toString())      // sub - user UUID
                .issuer(issuer)                                 // iss - "digital-banking-auth-service"
                .issuedAt(Date.from(now))                       // iat - current timestamp
                .expiration(Date.from(expiry))                  // exp - when token expires
                .claim("username", userPrincipal.getUsername()) // custom claim
                .claim("email", userPrincipal.getEmail())       // custom claim
                .claim("roles", roles)                          // custom claim
                .signWith(secretKey, Jwts.SIG.HS256)            // sign with HMAC-SHA256
                .compact();                                      // serialize to string
    }

    /**
     * Generate refresh token string.
     * 
     * NOTE: This is NOT a JWT - it's just a random string!
     * 
     * WHY NOT USE JWT FOR REFRESH TOKEN?
     * - Refresh tokens need to be revocable (logout, security breach)
     * - JWTs are stateless - can't revoke without blacklist
     * - Random string stored in DB = can simply delete to revoke
     * - Less data in refresh token = smaller attack surface
     * 
     * Format: uuid-uuid (e.g., "a1b2c3d4-...-x1y2z3w4-...")
     * Double UUID for extra entropy
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    // ==================== EXPIRATION HELPERS ====================

    /**
     * Get access token expiration in seconds (for API response).
     * Converts milliseconds to seconds for client convenience.
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    /**
     * Get refresh token expiry as Instant (for database storage).
     * Used when creating RefreshToken entity.
     */
    public Instant getRefreshTokenExpiryDate() {
        return Instant.now().plusMillis(refreshTokenExpiration);
    }

    // ==================== TOKEN PARSING - EXTRACT DATA ====================

    /**
     * Extract user ID from JWT token.
     * 
     * User ID is stored in 'sub' (subject) claim.
     * CALLED BY: JwtAuthenticationFilter to load user from database.
     * 
     * @param token JWT string (without "Bearer " prefix)
     * @return User's UUID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract username from JWT token.
     * Stored in custom 'username' claim.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * Extract email from JWT token.
     * Stored in custom 'email' claim.
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Extract roles from JWT token.
     * Stored as comma-separated string in custom 'roles' claim.
     */
    public String getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles", String.class);
    }

    /**
     * Get token expiration date.
     * Stored in 'exp' claim as Unix timestamp.
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    // ==================== TOKEN VALIDATION ====================

    /**
     * Validate JWT token - check signature and expiration.
     * 
     * CALLED BY: JwtAuthenticationFilter.doFilterInternal()
     * 
     * Returns true only if:
     * 1. Signature is valid (wasn't tampered with)
     * 2. Token hasn't expired
     * 3. Token is well-formed JWT
     * 
     * @param token JWT string to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);  // Will throw if invalid
            return true;
        } catch (SignatureException ex) {
            // Token was tampered with - signature doesn't match
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            // Not a valid JWT format (wrong structure)
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            // Token's exp claim is in the past
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            // JWT uses unsupported features
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            // Token string is null or empty
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired (for specific handling of expiry).
     * 
     * Different from validateToken() because:
     * - validateToken() returns false for expired tokens
     * - This method specifically tells you WHY it's invalid (expired)
     * - Useful for deciding whether to suggest refresh vs re-login
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException ex) {
            // parseToken throws ExpiredJwtException - token is definitely expired
            return true;
        }
    }

    // ==================== INTERNAL PARSING ====================

    /**
     * Parse JWT token and return claims (payload data).
     * 
     * This is the CORE parsing method used by all extraction methods.
     * 
     * Process:
     * 1. Split token into header.payload.signature
     * 2. Verify signature using our secret key
     * 3. Check expiration claim
     * 4. Return payload as Claims object
     * 
     * JJWT Library Usage:
     * - Jwts.parser(): Create parser instance
     * - verifyWith(secretKey): Set key for signature verification
     * - build(): Finalize parser configuration
     * - parseSignedClaims(token): Parse and verify in one step
     * - getPayload(): Extract claims from verified token
     * 
     * @param token JWT string to parse
     * @return Claims object containing all token claims
     * @throws Various JwtException subclasses if invalid
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)       // Set verification key
                .build()                      // Build parser
                .parseSignedClaims(token)     // Parse and verify
                .getPayload();                // Get claims map
    }
}
