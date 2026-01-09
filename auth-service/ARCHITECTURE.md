# Auth Service - Architecture & Build Guide

## 📚 Table of Contents

1. [Overview](#overview)
2. [Build Order & Reasoning](#build-order--reasoning)
3. [Step-by-Step Implementation](#step-by-step-implementation)
4. [Dependency Flow](#dependency-flow)
5. [File Creation Order](#file-creation-order)

---

## Overview

This document explains the **complete build process** for the JWT authentication service. It walks through every step taken, explaining:
- **What** was created
- **Why** it was created in that order
- **How** each component connects to others

### Why This Order Matters

Building an authentication system requires careful ordering because:
1. **Entities** must exist before repositories can query them
2. **Repositories** must exist before services can use them
3. **Security components** must exist before the controller can use them
4. **Configuration** ties everything together at the end

---

## Build Order & Reasoning

### The Dependency Chain

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BUILD ORDER (Top to Bottom)                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  STEP 1: pom.xml (Dependencies)                                             │
│     │    └── Why first? Nothing works without dependencies                  │
│     ▼                                                                       │
│  STEP 2: application.yml / application-local.yml (Configuration)            │
│     │    └── Why second? Defines database, JWT settings needed by code      │
│     ▼                                                                       │
│  STEP 3: Entities (User, Role, RefreshToken)                                │
│     │    └── Why third? Foundation - everything else depends on data model  │
│     ▼                                                                       │
│  STEP 4: DTOs (Request/Response objects)                                    │
│     │    └── Why fourth? Define API contract before implementation          │
│     ▼                                                                       │
│  STEP 5: Repositories (Database access layer)                               │
│     │    └── Why fifth? Need entities first, services need repositories     │
│     ▼                                                                       │
│  STEP 6: Exceptions (Error handling)                                        │
│     │    └── Why sixth? Services will throw these, need them defined        │
│     ▼                                                                       │
│  STEP 7: Security Components (JWT infrastructure)                           │
│     │    └── Why seventh? Core auth logic, services depend on this          │
│     ▼                                                                       │
│  STEP 8: Services (Business logic)                                          │
│     │    └── Why eighth? Uses repos, exceptions, security components        │
│     ▼                                                                       │
│  STEP 9: Configuration (Security config, data initializer)                  │
│     │    └── Why ninth? Wires security components together                  │
│     ▼                                                                       │
│  STEP 10: Controller (REST API endpoints)                                   │
│     │    └── Why tenth? Final layer, uses services to handle requests       │
│     ▼                                                                       │
│  STEP 11: Dockerfile & Docker Compose                                       │
│     │    └── Why last? Deployment config after code is complete             │
│     ▼                                                                       │
│  STEP 12: Tests & API Request files                                         │
│           └── Verify everything works correctly                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Implementation

### STEP 1: pom.xml - Dependencies (Foundation)

**Why first?** 
- Maven/Gradle must know what libraries to download
- IDE needs dependencies for code completion
- Compilation fails without required classes

**What was added:**
```xml
<!-- Core Spring Boot starters -->
spring-boot-starter-web          → REST API support
spring-boot-starter-security     → Spring Security framework
spring-boot-starter-data-jpa     → Database ORM
spring-boot-starter-validation   → Input validation (@Valid)
spring-boot-starter-actuator     → Health checks

<!-- Database drivers -->
postgresql                       → Production database
h2                              → Local development database

<!-- JWT Library (JJWT) -->
jjwt-api                        → JWT interfaces
jjwt-impl                       → JWT implementation
jjwt-jackson                    → JSON serialization for JWT

<!-- Code generation -->
lombok                          → Reduces boilerplate (@Getter, @Builder)
mapstruct                       → Object mapping (if needed)

<!-- Documentation -->
springdoc-openapi               → Swagger UI
```

---

### STEP 2: Configuration Files (application.yml)

**Why second?**
- Code will reference these properties (jwt.secret, jwt.expiration)
- Database connection strings needed
- Spring needs to know the server port

**Two files created:**
1. `application.yml` - Production (PostgreSQL, Docker)
2. `application-local.yml` - Development (H2, localhost)

**Key configurations:**
```yaml
server.port: 4005              # HTTP port for auth-service

# Database
spring.datasource.url          # Where to connect
spring.jpa.hibernate.ddl-auto  # Schema management

# JWT Settings (THE CORE OF AUTH)
jwt.secret                     # Signing key (Base64 encoded)
jwt.expiration                 # Access token lifetime (24h)
jwt.refresh-expiration         # Refresh token lifetime (7 days)
jwt.issuer                     # Token issuer identifier
```

---

### STEP 3: Entities (Data Model)

**Why third?**
- Entities define database tables
- Everything else (repos, services) depends on these
- JPA annotations tell Hibernate how to create tables

**Order within entities:**
1. `Role.java` - Simplest, no dependencies
2. `User.java` - References Role (ManyToMany)
3. `RefreshToken.java` - References User (ManyToOne)

**Reasoning:**
```
Role (independent)
  ↑
User (depends on Role)
  ↑
RefreshToken (depends on User)
```

---

### STEP 4: DTOs (Data Transfer Objects)

**Why fourth?**
- Define the API contract (what client sends/receives)
- Separate from entities (security - don't expose everything)
- Validation annotations go here (@NotBlank, @Email)

**Order within DTOs:**
1. `RegisterRequest.java` - User registration input
2. `LoginRequest.java` - Login credentials input
3. `RefreshTokenRequest.java` - Token refresh input
4. `AuthResponse.java` - Success response (tokens + user info)
5. `UserDto.java` - User info (without password)
6. `ChangePasswordRequest.java` - Password change input

**Why this order?**
- Request DTOs first (what comes IN)
- Response DTOs second (what goes OUT)
- Follows the user journey: register → login → use tokens

---

### STEP 5: Repositories (Data Access Layer)

**Why fifth?**
- Services need repositories to save/find data
- Repositories need entities to be defined
- Spring Data JPA generates implementation

**Order within repositories:**
1. `RoleRepository.java` - Find roles by name
2. `UserRepository.java` - Find users, update login status
3. `RefreshTokenRepository.java` - Token management

**Key methods explained:**
```java
// UserRepository
findByUsernameOrEmail()     → Login can use either
existsByUsername()          → Check duplicates during registration
updateLoginSuccess()        → Record last login time

// RefreshTokenRepository  
findByToken()               → Validate refresh token
revokeAllUserTokens()       → Logout from all devices
```

---

### STEP 6: Exceptions (Error Handling)

**Why sixth?**
- Services will throw exceptions
- Need to define them before services use them
- Global handler converts exceptions to API responses

**Order within exceptions:**
1. `AuthErrorCode.java` - Enum of all error codes (define vocabulary)
2. `AuthException.java` - Base auth exception
3. `InvalidTokenException.java` - Token-specific errors
4. `ResourceNotFoundException.java` - Not found errors
5. `ApiError.java` - Standard error response format
6. `GlobalExceptionHandler.java` - Catches & converts exceptions

**Why ApiError record?**
- Consistent error format for all endpoints
- Includes timestamp, status, code, message, path
- Frontend knows exactly what to expect

---

### STEP 7: Security Components (JWT Infrastructure)

**Why seventh?**
- Core authentication logic
- Services depend on JwtTokenProvider
- Must be defined before SecurityConfig uses them

**Order within security (CRITICAL):**

```
1. UserPrincipal.java (implements UserDetails)
   └── Why first? Spring Security needs UserDetails to represent authenticated user
   └── Wraps User entity with security information
   
2. CustomUserDetailsService.java (implements UserDetailsService)
   └── Why second? Needs UserPrincipal to create
   └── Spring Security calls this to load user from database
   
3. JwtTokenProvider.java
   └── Why third? Core JWT logic (generate/validate tokens)
   └── Needs no auth dependencies, just JWT library
   
4. JwtAuthenticationFilter.java (extends OncePerRequestFilter)
   └── Why fourth? Needs JwtTokenProvider and CustomUserDetailsService
   └── Intercepts every request to check for JWT
   
5. JwtAuthenticationEntryPoint.java
   └── Why fifth? Handles "not authenticated" errors
   └── Returns JSON instead of redirect
   
6. JwtAccessDeniedHandler.java
   └── Why sixth? Handles "not authorized" errors (403)
   └── Returns JSON instead of error page
```

**Flow diagram:**
```
HTTP Request
    │
    ▼
JwtAuthenticationFilter
    │
    ├── Extract token from "Authorization: Bearer xxx"
    │
    ├── JwtTokenProvider.validateToken(token)
    │   └── Parse JWT, check signature, check expiry
    │
    ├── JwtTokenProvider.getUserIdFromToken(token)
    │   └── Extract user ID from JWT claims
    │
    ├── CustomUserDetailsService.loadUserById(userId)
    │   └── Load User from database
    │   └── Convert to UserPrincipal
    │
    └── Set Authentication in SecurityContext
            │
            ▼
        Controller (user is authenticated)
```

---

### STEP 8: Services (Business Logic)

**Why eighth?**
- Uses repositories, exceptions, security components
- All dependencies must exist first
- Contains the actual authentication logic

**Files:**
1. `AuthService.java` - Interface (contract)
2. `AuthServiceImpl.java` - Implementation

**Why interface + implementation?**
- Dependency Injection best practice
- Easy to mock in tests
- Can swap implementations

**Key methods flow:**

```
register():
    1. Check if username exists → throw if yes
    2. Check if email exists → throw if yes
    3. Get ROLE_USER from database
    4. Create User entity with encoded password
    5. Save to database
    6. Generate access token (JWT)
    7. Create refresh token (save to DB)
    8. Return AuthResponse

login():
    1. AuthenticationManager.authenticate()
       └── Calls CustomUserDetailsService.loadUserByUsername()
       └── Compares password with BCrypt
    2. Update last login time
    3. Generate access token
    4. Create refresh token
    5. Return AuthResponse

refreshToken():
    1. Find refresh token in database
    2. Validate (not expired, not revoked)
    3. Revoke old token (token rotation!)
    4. Generate new access token
    5. Create new refresh token
    6. Return AuthResponse
```

---

### STEP 9: Configuration (Wiring Everything)

**Why ninth?**
- Security components exist, now wire them together
- Configure which endpoints need authentication
- Set up password encoder, authentication manager

**Order within config:**
1. `SecurityConfig.java` - Main security configuration
2. `DataInitializer.java` - Seeds default roles
3. `OpenApiConfig.java` - Swagger documentation

**SecurityConfig explained:**
```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .csrf(disable)                    // Stateless = no CSRF needed
        .sessionManagement(STATELESS)     // No server sessions
        .authorizeHttpRequests(auth -> {
            // Public endpoints (no token needed)
            auth.requestMatchers("/api/auth/register").permitAll();
            auth.requestMatchers("/api/auth/login").permitAll();
            auth.requestMatchers("/api/auth/refresh").permitAll();
            
            // Protected endpoints (token required)
            auth.anyRequest().authenticated();
        })
        // Add our JWT filter BEFORE Spring's default filter
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
}
```

**Why DataInitializer?**
- Roles must exist before users can be assigned roles
- Runs on application startup
- Creates ROLE_USER, ROLE_ADMIN, etc.

---

### STEP 10: Controller (REST API)

**Why tenth?**
- Final layer - receives HTTP requests
- Uses AuthService to do actual work
- All dependencies are now ready

**Endpoint flow:**
```
POST /api/auth/register
    │
    ▼
AuthController.register(RegisterRequest)
    │
    ├── @Valid validates input (password rules, email format)
    │
    └── authService.register(request)
            │
            └── Returns AuthResponse with tokens
```

---

### STEP 11: Dockerfile & Docker Compose

**Why eleventh?**
- Code is complete, now package for deployment
- Dockerfile builds the service
- docker-compose.yml orchestrates with database

---

### STEP 12: Tests & HTTP Request Files

**Why last?**
- Everything must work before testing
- Tests verify the implementation
- HTTP files help manual testing

---

## Dependency Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         DEPENDENCY GRAPH                                  │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  pom.xml ─────────────────────────────────────────────────────────────┐  │
│     │                                                                  │  │
│     ▼                                                                  │  │
│  application.yml ──────────────────────────────────────────────────┐  │  │
│     │                                                               │  │  │
│     ▼                                                               │  │  │
│  ┌─────────────────────────────────────────────────────────────┐   │  │  │
│  │                      ENTITIES                                │   │  │  │
│  │  Role ◄──── User ◄──── RefreshToken                         │   │  │  │
│  └─────────────────────────────────────────────────────────────┘   │  │  │
│     │                                                               │  │  │
│     ▼                                                               │  │  │
│  ┌─────────────────────────────────────────────────────────────┐   │  │  │
│  │                      REPOSITORIES                            │   │  │  │
│  │  RoleRepository  UserRepository  RefreshTokenRepository      │   │  │  │
│  └─────────────────────────────────────────────────────────────┘   │  │  │
│     │                                                               │  │  │
│     ├────────────────────────────────────┐                         │  │  │
│     ▼                                    ▼                         │  │  │
│  ┌───────────────────┐    ┌─────────────────────────────────────┐  │  │  │
│  │    EXCEPTIONS     │    │       SECURITY COMPONENTS           │  │  │  │
│  │  AuthErrorCode    │    │  UserPrincipal                      │  │  │  │
│  │  AuthException    │    │  CustomUserDetailsService ◄─────────┼──┘  │  │
│  │  InvalidToken...  │    │  JwtTokenProvider ◄─────────────────┼─────┘  │
│  │  ApiError         │    │  JwtAuthenticationFilter            │        │
│  │  GlobalHandler    │    │  JwtAuthEntryPoint                  │        │
│  └───────────────────┘    │  JwtAccessDeniedHandler             │        │
│     │                     └─────────────────────────────────────┘        │
│     │                                    │                               │
│     └────────────────┬───────────────────┘                               │
│                      ▼                                                   │
│  ┌─────────────────────────────────────────────────────────────┐        │
│  │                      SERVICES                                │        │
│  │  AuthService (interface)                                     │        │
│  │  AuthServiceImpl (implementation)                            │        │
│  └─────────────────────────────────────────────────────────────┘        │
│     │                                                                    │
│     ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐        │
│  │                    CONFIGURATION                             │        │
│  │  SecurityConfig ◄── wires all security components            │        │
│  │  DataInitializer ◄── seeds roles on startup                  │        │
│  │  OpenApiConfig ◄── Swagger documentation                     │        │
│  └─────────────────────────────────────────────────────────────┘        │
│     │                                                                    │
│     ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐        │
│  │                     CONTROLLER                               │        │
│  │  AuthController ◄── REST API endpoints                       │        │
│  └─────────────────────────────────────────────────────────────┘        │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## File Creation Order

Here's the exact order files were created:

| Order | File | Reason |
|-------|------|--------|
| 1 | `pom.xml` | Dependencies must be defined first |
| 2 | `application.yml` | Production configuration |
| 3 | `application-local.yml` | Development configuration |
| 4 | `entity/Role.java` | Simplest entity, no dependencies |
| 5 | `entity/User.java` | Depends on Role |
| 6 | `entity/RefreshToken.java` | Depends on User |
| 7 | `dto/RegisterRequest.java` | Input validation for registration |
| 8 | `dto/LoginRequest.java` | Input for login |
| 9 | `dto/RefreshTokenRequest.java` | Input for token refresh |
| 10 | `dto/AuthResponse.java` | Output with tokens |
| 11 | `dto/UserDto.java` | User info response |
| 12 | `dto/ChangePasswordRequest.java` | Password change input |
| 13 | `repository/UserRepository.java` | Database access for users |
| 14 | `repository/RoleRepository.java` | Database access for roles |
| 15 | `repository/RefreshTokenRepository.java` | Database access for tokens |
| 16 | `exception/AuthErrorCode.java` | Error codes enum |
| 17 | `exception/AuthException.java` | Base exception |
| 18 | `exception/ResourceNotFoundException.java` | 404 errors |
| 19 | `exception/InvalidTokenException.java` | Token errors |
| 20 | `exception/ApiError.java` | Error response format |
| 21 | `exception/GlobalExceptionHandler.java` | Exception to response converter |
| 22 | `security/UserPrincipal.java` | Spring Security user wrapper |
| 23 | `security/CustomUserDetailsService.java` | Load user from database |
| 24 | `security/JwtTokenProvider.java` | JWT generation/validation |
| 25 | `security/JwtAuthenticationFilter.java` | Request interceptor |
| 26 | `security/JwtAuthenticationEntryPoint.java` | 401 handler |
| 27 | `security/JwtAccessDeniedHandler.java` | 403 handler |
| 28 | `service/AuthService.java` | Service interface |
| 29 | `service/impl/AuthServiceImpl.java` | Service implementation |
| 30 | `config/SecurityConfig.java` | Spring Security setup |
| 31 | `config/DataInitializer.java` | Role seeding |
| 32 | `config/OpenApiConfig.java` | Swagger setup |
| 33 | `controller/AuthController.java` | REST endpoints |
| 34 | `Dockerfile` | Container build |
| 35 | `docker-compose.yml` (updated) | Orchestration |
| 36 | `auth-requests.http` | API testing |
| 37 | `AuthControllerIntegrationTest.java` | Automated tests |

---

## Summary

The build follows a **bottom-up approach**:

1. **Foundation** (dependencies, config)
2. **Data Layer** (entities, repositories)
3. **Error Handling** (exceptions)
4. **Security Infrastructure** (JWT components)
5. **Business Logic** (services)
6. **Wiring** (configuration classes)
7. **API Layer** (controller)
8. **Deployment** (Docker)
9. **Verification** (tests)

Each layer depends on the layers below it. You cannot build a controller without a service, you cannot build a service without repositories, and you cannot build repositories without entities.

This document should be read alongside the code comments in each file for complete understanding.
