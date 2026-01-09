package com.bankingmanagement.authservice.exception;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                              AUTH ERROR CODE ENUM                                         ║
║                                                                                           ║
║  BUILD ORDER: STEP 6a of 12 (First Exception Component)                                  ║
║  PREVIOUS STEP: Repositories (Step 5)                                                    ║
║  NEXT STEP: Exception classes (AuthException, InvalidTokenException, etc.)               ║
║                                                                                           ║
║  WHY EXCEPTIONS COME AFTER REPOSITORIES:                                                 ║
║  - Repositories can throw exceptions (not found, duplicates, etc.)                       ║
║  - We need error codes before building the service layer                                 ║
║  - Service layer will throw these exceptions based on business rules                     ║
║                                                                                           ║
║  WHY START WITH ERROR CODES (not exception classes)?                                     ║
║  - Error codes are referenced by ALL exception classes                                   ║
║  - They define the "vocabulary" of errors                                                ║
║  - Must exist before exception classes can use them                                      ║
║                                                                                           ║
║  ERROR CODE DESIGN:                                                                       ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Code Format: AUTH + XX + Y                                                         │ ║
║  │                                                                                     │ ║
║  │  AUTH00X = General authentication errors                                            │ ║
║  │  AUTH01X = Token-related errors                                                     │ ║
║  │  AUTH02X = Registration errors                                                      │ ║
║  │  AUTH03X = Password errors                                                          │ ║
║  │  AUTH04X = Authorization errors                                                     │ ║
║  │  AUTH05X = Resource not found errors                                                │ ║
║  │                                                                                     │ ║
║  │  WHY USE CODES?                                                                     │ ║
║  │  - Stable across languages (i18n friendly)                                          │ ║
║  │  - Easy to search in logs                                                           │ ║
║  │  - Clients can handle specific errors programmatically                              │ ║
║  │  - Messages can change without breaking client code                                 │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  EXAMPLE API RESPONSE WITH ERROR CODE:                                                   ║
║  {                                                                                        ║
║    "timestamp": "2025-01-27T10:30:00Z",                                                  ║
║    "status": 401,                                                                        ║
║    "error": "Unauthorized",                                                              ║
║    "code": "AUTH002",           ← Client checks this                                    ║
║    "message": "Invalid credentials",                                                     ║
║    "path": "/api/auth/login"                                                            ║
║  }                                                                                        ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

/**
 * Enum defining authentication error codes.
 * 
 * Each error code has:
 * - code: Stable identifier for programmatic handling (AUTH001)
 * - defaultMessage: Human-readable message (can be overridden)
 * 
 * CLIENT USAGE:
 * if (response.code === "AUTH002") {
 *   showError("Please check your username and password");
 * } else if (response.code === "AUTH004") {
 *   showError("Your account is locked. Please contact support.");
 * }
 */
public enum AuthErrorCode {

    // ========================= AUTHENTICATION ERRORS (AUTH00X) =========================
    /*
     * General login/authentication failures.
     * These prevent the user from getting a token.
     */

    /** Generic auth failure (use more specific codes when possible) */
    AUTHENTICATION_FAILED("AUTH001", "Authentication failed"),

    /** Wrong username/email or password */
    INVALID_CREDENTIALS("AUTH002", "Invalid username/email or password"),

    /** Account exists but is disabled by admin */
    ACCOUNT_DISABLED("AUTH003", "User account is disabled"),

    /** Account locked due to too many failed login attempts */
    ACCOUNT_LOCKED("AUTH004", "User account is locked"),

    /** Account validity period has ended */
    ACCOUNT_EXPIRED("AUTH005", "User account has expired"),

    /** Password needs to be changed (forced rotation) */
    CREDENTIALS_EXPIRED("AUTH006", "User credentials have expired"),

    // ========================= TOKEN ERRORS (AUTH01X) =========================
    /*
     * JWT or Refresh Token validation failures.
     * These reject API requests with bad/old tokens.
     */

    /** Token format is invalid or signature doesn't match */
    INVALID_TOKEN("AUTH010", "Invalid token"),

    /** Token was valid but has passed its expiration time */
    TOKEN_EXPIRED("AUTH011", "Token has expired"),

    /** Token was explicitly revoked (user logged out) */
    TOKEN_REVOKED("AUTH012", "Token has been revoked"),

    /** Refresh token not found in database */
    REFRESH_TOKEN_NOT_FOUND("AUTH013", "Refresh token not found"),

    /** Refresh token exists but is invalid (revoked/expired) */
    INVALID_REFRESH_TOKEN("AUTH014", "Invalid refresh token"),

    // ========================= REGISTRATION ERRORS (AUTH02X) =========================
    /*
     * User registration failures.
     * Prevent duplicate accounts and invalid registrations.
     */

    /** Username is already taken */
    USERNAME_ALREADY_EXISTS("AUTH020", "Username already exists"),

    /** Email is already registered */
    EMAIL_ALREADY_EXISTS("AUTH021", "Email already exists"),

    /** Generic registration failure */
    REGISTRATION_FAILED("AUTH022", "Registration failed"),

    // ========================= PASSWORD ERRORS (AUTH03X) =========================
    /*
     * Password change/reset failures.
     * Ensure secure password management.
     */

    /** Current password verification failed */
    INVALID_CURRENT_PASSWORD("AUTH030", "Current password is incorrect"),

    /** newPassword != confirmPassword */
    PASSWORD_MISMATCH("AUTH031", "New password and confirmation do not match"),

    /** User is trying to reuse their current password */
    PASSWORD_SAME_AS_OLD("AUTH032", "New password must be different from current password"),

    // ========================= AUTHORIZATION ERRORS (AUTH04X) =========================
    /*
     * Permission/access control failures.
     * User is authenticated but not authorized for the resource.
     */

    /** Generic access denied */
    ACCESS_DENIED("AUTH040", "Access denied"),

    /** User lacks required role/permission */
    INSUFFICIENT_PERMISSIONS("AUTH041", "Insufficient permissions"),

    // ========================= RESOURCE ERRORS (AUTH05X) =========================
    /*
     * Entity not found errors.
     * Requested user/role doesn't exist.
     */

    /** User with given ID/username/email doesn't exist */
    USER_NOT_FOUND("AUTH050", "User not found"),

    /** Role with given name doesn't exist */
    ROLE_NOT_FOUND("AUTH051", "Role not found");

    // ========================= ENUM FIELDS =========================

    private final String code;
    private final String defaultMessage;

    AuthErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Get the error code string (e.g., "AUTH002").
     * This is what clients use for programmatic error handling.
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the default human-readable message.
     * Can be overridden when throwing the exception.
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
