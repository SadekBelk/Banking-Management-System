package com.bankingmanagement.authservice.dto;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                                LOGIN REQUEST DTO                                          ║
║                                                                                           ║
║  BUILD ORDER: STEP 4b of 12 (Second DTO)                                                 ║
║  PREVIOUS STEP: RegisterRequest.java                                                     ║
║  NEXT STEP: AuthResponse.java (what we return after login)                               ║
║                                                                                           ║
║  WHY THIS DTO IS SO SIMPLE:                                                              ║
║  - Login only needs credentials (username/email + password)                              ║
║  - More validation happens in the service layer (against database)                       ║
║  - We intentionally don't validate format here (let user try, get clear error)           ║
║                                                                                           ║
║  DESIGN DECISION: usernameOrEmail (not separate fields)                                  ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Option A: Two separate fields                                                      │ ║
║  │  {                                                                                  │ ║
║  │    "username": "john_doe",     // Use this OR                                      │ ║
║  │    "email": "john@example.com" // Use this                                         │ ║
║  │  }                                                                                  │ ║
║  │  Problem: Confusing API - which one to use? What if both provided?                  │ ║
║  │                                                                                     │ ║
║  │  Option B: Single unified field (OUR CHOICE)                                        │ ║
║  │  {                                                                                  │ ║
║  │    "usernameOrEmail": "john_doe"      // Username works                            │ ║
║  │  }                                                                                  │ ║
║  │  OR                                                                                 │ ║
║  │  {                                                                                  │ ║
║  │    "usernameOrEmail": "john@email.com" // Email works too                          │ ║
║  │  }                                                                                  │ ║
║  │  Benefit: Simple, intuitive, one field to fill                                      │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  HOW THE SERVICE LAYER HANDLES IT:                                                       ║
║  1. Check if usernameOrEmail contains '@'                                                ║
║  2. If yes: Look up by email                                                            ║
║  3. If no: Look up by username                                                          ║
║  4. Verify password against found user                                                  ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user login requests.
 * Accepts either username or email for authentication.
 * 
 * USAGE EXAMPLES:
 * 
 * Login with username:
 * {
 *   "usernameOrEmail": "john_doe",
 *   "password": "MySecureP@ss123"
 * }
 * 
 * Login with email:
 * {
 *   "usernameOrEmail": "john@example.com",
 *   "password": "MySecureP@ss123"
 * }
 * 
 * Both work identically - service layer determines which lookup to use.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * Username or email address to identify the user.
     * 
     * WHY NO FORMAT VALIDATION HERE?
     * - We don't want to assume if it's username or email
     * - The service layer will check the database
     * - Better UX: Let user type, give meaningful error if not found
     */
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    /**
     * User's password.
     * 
     * WHY NO PASSWORD PATTERN VALIDATION HERE?
     * - Existing users might have old passwords that don't meet current rules
     * - We just need to check if it matches what's in the database
     * - Validation rules are for CREATING passwords, not CHECKING them
     */
    @NotBlank(message = "Password is required")
    private String password;
}
