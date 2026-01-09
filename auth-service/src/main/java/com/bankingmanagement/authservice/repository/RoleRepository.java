package com.bankingmanagement.authservice.repository;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                              ROLE REPOSITORY                                              ║
║                                                                                           ║
║  BUILD ORDER: STEP 5a of 12 (First Repository)                                           ║
║  PREVIOUS STEP: DTOs (Step 4) - API contract defined                                     ║
║  NEXT STEP: UserRepository, RefreshTokenRepository                                       ║
║                                                                                           ║
║  WHY REPOSITORIES COME AFTER DTOs:                                                       ║
║  - DTOs define WHAT data we need                                                         ║
║  - Repositories define HOW we get that data from the database                            ║
║  - We needed to know the domain model (entities) and API needs (DTOs) first              ║
║                                                                                           ║
║  WHAT IS A REPOSITORY?                                                                   ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │  Repository = Interface that provides CRUD operations on entities                   │ ║
║  │                                                                                     │ ║
║  │  JpaRepository<Role, UUID> provides:                                                │ ║
║  │  - save(entity): Insert or update                                                   │ ║
║  │  - findById(id): Get by primary key                                                 │ ║
║  │  - findAll(): Get all records                                                       │ ║
║  │  - deleteById(id): Remove by primary key                                            │ ║
║  │  - count(): Total records                                                           │ ║
║  │  - And many more...                                                                 │ ║
║  │                                                                                     │ ║
║  │  WE JUST ADD CUSTOM QUERIES that JPA doesn't provide automatically                  │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  WHY RoleRepository IS SIMPLE:                                                           ║
║  - Roles are mostly static data (ROLE_USER, ROLE_ADMIN, etc.)                           ║
║  - Just need to look them up by name                                                    ║
║  - Rarely created/deleted after initial setup                                           ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import com.bankingmanagement.authservice.entity.Role;
import com.bankingmanagement.authservice.entity.Role.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Role entity operations.
 * 
 * This is the simplest repository - roles are mostly static lookup data.
 * Used primarily during user registration to assign default role.
 * 
 * SPRING DATA JPA MAGIC:
 * Just by declaring method signatures, Spring generates the implementation!
 * 
 * findByName(RoleName name) becomes:
 * SELECT * FROM roles WHERE name = ?
 * 
 * existsByName(RoleName name) becomes:
 * SELECT EXISTS(SELECT 1 FROM roles WHERE name = ?)
 */
@Repository  // Marks this as a Spring-managed repository bean
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find a role by its name enum.
     * 
     * Used when assigning roles to users:
     * Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
     *     .orElseThrow(() -> new RuntimeException("Role not found"));
     * user.getRoles().add(userRole);
     * 
     * Returns Optional because role might not exist in database yet.
     * 
     * @param name The RoleName enum value (e.g., ROLE_USER, ROLE_ADMIN)
     * @return Optional containing the Role if found, empty otherwise
     */
    Optional<Role> findByName(RoleName name);

    /**
     * Check if a role exists without fetching the entity.
     * 
     * Used during application startup to check if roles need to be created.
     * More efficient than findByName when you just need to check existence.
     * 
     * @param name The RoleName enum value to check
     * @return true if a role with this name exists in the database
     */
    boolean existsByName(RoleName name);
}
