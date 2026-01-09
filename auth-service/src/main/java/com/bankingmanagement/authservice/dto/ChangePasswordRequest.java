package com.bankingmanagement.authservice.dto;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                           CHANGE PASSWORD REQUEST DTO                                     ║
║                                                                                           ║
║  BUILD ORDER: STEP 4f of 12 (Sixth and Final DTO)                                        ║
║  PREVIOUS STEP: UserDto.java                                                             ║
║  NEXT STEP: Repositories (Step 5) - Data access layer                                    ║
║                                                                                           ║
║  WHAT THIS DTO DOES:                                                                     ║
║  - Carries password change data from authenticated user                                  ║
║  - Requires BOTH current password (verification) AND new password                        ║
║  - Includes confirmation to prevent typos                                                ║
║                                                                                           ║
║  WHY THREE FIELDS (not just new password)?                                               ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  SECURITY REQUIREMENT: Prove you know the current password                          │ ║
║  │                                                                                     │ ║
║  │  Scenario: User leaves computer unlocked, attacker finds it                         │ ║
║  │                                                                                     │ ║
║  │  WITHOUT currentPassword field:                                                     │ ║
║  │  - Attacker just sets new password                                                  │ ║
║  │  - Real user is locked out                                                          │ ║
║  │                                                                                     │ ║
║  │  WITH currentPassword field:                                                        │ ║
║  │  - Attacker can't change password without knowing current one                       │ ║
║  │  - Even with valid JWT token, can't change password                                 │ ║
║  │                                                                                     │ ║
║  │  WHY confirmPassword?                                                               │ ║
║  │  - Password fields are masked (●●●●●●●)                                            │ ║
║  │  - Easy to make typos without noticing                                              │ ║
║  │  - Confirmation prevents locking yourself out with typo'd password                  │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  VALIDATION RULES:                                                                       ║
║  - currentPassword: Just required (no format check - it's whatever they set before)     ║
║  - newPassword: Full validation (8+ chars, uppercase, lowercase, digit, special)        ║
║  - confirmPassword: Must match newPassword (checked in service layer)                   ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for password change requests.
 * 
 * REQUIRES AUTHENTICATION: User must be logged in with valid JWT.
 * REQUIRES CURRENT PASSWORD: Even with valid token, must prove identity.
 * 
 * Used by: POST /api/auth/change-password
 * 
 * EXAMPLE REQUEST:
 * POST /api/auth/change-password
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
 * Content-Type: application/json
 * 
 * {
 *   "currentPassword": "OldP@ssword123",
 *   "newPassword": "NewSecureP@ss456",
 *   "confirmPassword": "NewSecureP@ss456"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    /**
     * User's current password for verification.
     * 
     * WHY NO FORMAT VALIDATION?
     * - User might have set password before current rules existed
     * - We just need to verify it matches what's in the database
     * - Don't want to reject valid current passwords
     */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /**
     * The new password to set.
     * 
     * MUST meet all security requirements:
     * - 8-100 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character (@$!%*?&)
     */
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    private String newPassword;

    /**
     * Confirmation of the new password.
     * 
     * Service layer validates: confirmPassword.equals(newPassword)
     * If they don't match, returns error before touching the database.
     */
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
