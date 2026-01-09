package com.bankingmanagement.authservice.config;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                             DATA INITIALIZER                                              ║
║                                                                                           ║
║  BUILD ORDER: STEP 9b of 12 (Configuration - Database Seeding)                           ║
║  PREVIOUS STEP: SecurityConfig (security is wired up, now need initial data)             ║
║  NEXT STEP: OpenApiConfig (API documentation configuration)                              ║
║                                                                                           ║
║  WHY DO WE NEED THIS?                                                                    ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  PROBLEM: Chicken and Egg                                                                ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  1. Users need Roles (many-to-many relationship)                                    │ ║
║  │  2. Registration assigns ROLE_USER to new users                                     │ ║
║  │  3. But ROLE_USER must exist in database first!                                     │ ║
║  │                                                                                     │ ║
║  │  Without DataInitializer:                                                           │ ║
║  │  - User registers                                                                   │ ║
║  │  - Code tries to find ROLE_USER                                                     │ ║
║  │  - Role doesn't exist → "Resource not found" error                                  │ ║
║  │                                                                                     │ ║
║  │  With DataInitializer:                                                              │ ║
║  │  - Application starts                                                               │ ║
║  │  - CommandLineRunner seeds all roles                                                │ ║
║  │  - Now registration works!                                                          │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHAT IS CommandLineRunner?                                                              ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  Spring Boot interface - code to run AFTER application context is ready.            │ ║
║  │                                                                                     │ ║
║  │  Application Startup Sequence:                                                      │ ║
║  │  ┌──────────────────────────────────────────────────────────────────────────┐      │ ║
║  │  │ 1. JVM starts                                                            │      │ ║
║  │  │ 2. Spring Boot main() runs                                               │      │ ║
║  │  │ 3. ApplicationContext created                                            │      │ ║
║  │  │ 4. All @Bean methods called                                              │      │ ║
║  │  │ 5. All @Configuration classes processed                                  │      │ ║
║  │  │ 6. ★ CommandLineRunner.run() called ★  <-- OUR CODE RUNS HERE            │      │ ║
║  │  │ 7. Application ready to receive requests                                 │      │ ║
║  │  └──────────────────────────────────────────────────────────────────────────┘      │ ║
║  │                                                                                     │ ║
║  │  Perfect for: Database seeding, cache warming, validation                           │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  IDEMPOTENT DESIGN:                                                                      ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  "Idempotent" = Running multiple times has same effect as running once.             │ ║
║  │                                                                                     │ ║
║  │  Our code checks: if (!roleRepository.existsByName(roleName))                       │ ║
║  │                                                                                     │ ║
║  │  This means:                                                                        │ ║
║  │  - First startup: Creates all 5 roles                                               │ ║
║  │  - Second startup: Sees roles exist, does nothing                                   │ ║
║  │  - Restart after crash: No duplicate roles                                          │ ║
║  │  - Safe to run anytime!                                                             │ ║
║  │                                                                                     │ ║
║  │  Alternative approaches:                                                            │ ║
║  │  - Flyway/Liquibase migrations (better for production)                              │ ║
║  │  - data.sql file (runs on every startup - problematic)                              │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.entity.Role;
import com.bankingmanagement.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Database initialization configuration.
 * Seeds default roles on application startup.
 * 
 * Runs automatically when Spring Boot starts.
 * Idempotent - safe to run multiple times.
 * 
 * @Configuration marks this as Spring configuration class
 * @RequiredArgsConstructor injects RoleRepository
 * @Slf4j provides logging
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;

    /**
     * Initialize roles in database on startup.
     * 
     * @Bean creates a CommandLineRunner that Spring Boot executes after startup.
     * 
     * Process:
     * 1. Iterate through all RoleName enum values
     * 2. Check if role already exists
     * 3. If not, create it with description
     * 
     * @return CommandLineRunner instance (lambda)
     */
    @Bean
    public CommandLineRunner initRoles() {
        // Lambda implementing CommandLineRunner.run(String... args)
        return args -> {
            log.info("Initializing default roles...");

            // Iterate all enum values: ROLE_USER, ROLE_CUSTOMER, ROLE_TELLER, ROLE_MANAGER, ROLE_ADMIN
            for (Role.RoleName roleName : Role.RoleName.values()) {
                
                // Idempotent check - only create if doesn't exist
                if (!roleRepository.existsByName(roleName)) {
                    Role role = Role.builder()
                            .name(roleName)
                            .description(getDescriptionForRole(roleName))
                            .build();
                    
                    roleRepository.save(role);
                    log.info("Created role: {}", roleName);
                }
                // If role exists, silently skip (idempotent)
            }

            log.info("Role initialization completed.");
        };
    }

    /**
     * Get human-readable description for each role.
     * 
     * Java 21 switch expression with pattern matching.
     * More concise than traditional switch statement.
     * 
     * @param roleName The role enum value
     * @return Description string for the role
     */
    private String getDescriptionForRole(Role.RoleName roleName) {
        return switch (roleName) {
            case ROLE_USER -> "Basic authenticated user";
            case ROLE_CUSTOMER -> "Bank customer with account access";
            case ROLE_TELLER -> "Bank teller with transaction capabilities";
            case ROLE_MANAGER -> "Branch manager with elevated permissions";
            case ROLE_ADMIN -> "System administrator with full access";
        };
    }
}
