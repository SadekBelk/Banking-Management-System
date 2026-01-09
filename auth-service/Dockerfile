# ╔══════════════════════════════════════════════════════════════════════════════════════════╗
# ║                            AUTH SERVICE DOCKERFILE                                        ║
# ║                                                                                           ║
# ║  BUILD ORDER: STEP 11a of 12 (Containerization)                                           ║
# ║  PREVIOUS STEP: REST Controller (all application code complete)                           ║
# ║  NEXT STEP: docker-compose.yml (orchestrate with other services)                          ║
# ║                                                                                           ║
# ║  PURPOSE: Package auth-service as a Docker container for deployment                       ║
# ╠══════════════════════════════════════════════════════════════════════════════════════════╣
# ║                                                                                           ║
# ║  MULTI-STAGE BUILD PATTERN:                                                              ║
# ║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
# ║  │                                                                                     │ ║
# ║  │  Stage 1: BUILD                     Stage 2: RUNTIME                                │ ║
# ║  │  ┌─────────────────────────┐        ┌─────────────────────────┐                     │ ║
# ║  │  │ maven:3.9.6-temurin-21  │   →    │ temurin:21-jre-alpine   │                     │ ║
# ║  │  │ (~800MB)                │        │ (~200MB)                 │                     │ ║
# ║  │  │                         │        │                         │                     │ ║
# ║  │  │ • Full JDK              │        │ • JRE only (no JDK)     │                     │ ║
# ║  │  │ • Maven                 │        │ • Just the JAR          │                     │ ║
# ║  │  │ • Source code           │        │ • Alpine Linux (small)  │                     │ ║
# ║  │  │ • Dependencies          │        │ • Non-root user         │                     │ ║
# ║  │  │                         │        │                         │                     │ ║
# ║  │  │ OUTPUT: app.jar         │   →    │ INPUT: app.jar          │                     │ ║
# ║  │  └─────────────────────────┘        └─────────────────────────┘                     │ ║
# ║  │                                                                                     │ ║
# ║  │  WHY: Build tools stay in build stage, final image is small & secure                │ ║
# ║  │                                                                                     │ ║
# ║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
# ║                                                                                           ║
# ║  BUILD COMMAND:                                                                          ║
# ║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
# ║  │  # From project root (Digital-Banking-management/):                                 │ ║
# ║  │  docker build -f auth-service/Dockerfile -t auth-service:latest .                   │ ║
# ║  │                                                                                     │ ║
# ║  │  # Or use docker-compose:                                                           │ ║
# ║  │  docker compose --profile auth up --build                                           │ ║
# ║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
# ║                                                                                           ║
# ╚══════════════════════════════════════════════════════════════════════════════════════════╝

# =====================================================================
# STAGE 1: BUILD
# =====================================================================
# Full JDK + Maven image for compiling the application.
# This stage is discarded after extracting the JAR.
#
# Base image: maven:3.9.6-eclipse-temurin-21
# - Maven 3.9.6 for dependency resolution and building
# - Eclipse Temurin JDK 21 (formerly AdoptOpenJDK)
# - ~800MB image size (acceptable for build stage)
# =====================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory inside container
WORKDIR /app

# ---------------------------------------------------------------------
# LAYER CACHING OPTIMIZATION
# ---------------------------------------------------------------------
# Copy pom.xml FIRST, then download dependencies.
# This layer gets cached unless pom.xml changes.
# 
# Without this:
#   - Every code change → re-download all dependencies
#   - Build time: ~5 minutes
#
# With this:
#   - Only pom.xml change → re-download dependencies
#   - Code change only → use cached dependencies
#   - Build time: ~30 seconds (after first build)
# ---------------------------------------------------------------------
COPY auth-service/pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code and build
# -DskipTests: Tests run separately in CI/CD, not during image build
# -B: Batch mode (no interactive prompts, CI-friendly)
COPY auth-service/src ./src
RUN mvn clean package -DskipTests -B

# =====================================================================
# STAGE 2: RUNTIME
# =====================================================================
# Minimal JRE image for running the application.
# Only contains what's needed to run the JAR.
#
# Base image: eclipse-temurin:21-jre-alpine
# - JRE only (no JDK tools like javac)
# - Alpine Linux (5MB base, extremely small)
# - ~200MB total image size
#
# WHY Alpine?
# - Smaller attack surface (fewer packages)
# - Faster image pulls
# - Lower storage costs
# =====================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# ---------------------------------------------------------------------
# SECURITY: Non-root User
# ---------------------------------------------------------------------
# Never run containers as root!
#
# Create dedicated user and group:
# - addgroup -S spring: Create system group "spring"
# - adduser -S spring -G spring: Create system user "spring" in that group
#
# WHY:
# - If container is compromised, attacker has limited privileges
# - Follows principle of least privilege
# - Required for many Kubernetes deployments
# ---------------------------------------------------------------------
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from build stage
# --from=builder: Reference the "builder" stage
# target/*.jar: Maven output location (spring-boot-maven-plugin creates FAT JAR)
COPY --from=builder /app/target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user (all subsequent commands run as this user)
USER spring

# ---------------------------------------------------------------------
# EXPOSE PORT
# ---------------------------------------------------------------------
# Document the port (doesn't actually publish it)
# Port mapping happens in docker-compose.yml or docker run -p
#
# auth-service HTTP port: 4005 (see application.yml)
# ---------------------------------------------------------------------
EXPOSE 4005

# ---------------------------------------------------------------------
# HEALTH CHECK
# ---------------------------------------------------------------------
# Docker daemon periodically checks container health.
# If unhealthy, orchestrator can restart the container.
#
# Options:
# --interval=30s    Check every 30 seconds
# --timeout=10s     Wait max 10s for response
# --start-period=40s Wait 40s before first check (app startup time)
# --retries=3       3 failures = unhealthy
#
# Command: wget to Spring Boot Actuator health endpoint
# Returns: 0 (healthy) or 1 (unhealthy)
# ---------------------------------------------------------------------
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:4005/actuator/health || exit 1

# ---------------------------------------------------------------------
# ENTRYPOINT
# ---------------------------------------------------------------------
# Command to run when container starts.
#
# --enable-preview: Enable Java 21 preview features (used in project)
# -jar app.jar: Run the Spring Boot fat JAR
#
# ENTRYPOINT vs CMD:
# - ENTRYPOINT: Always executes (can be overridden with --entrypoint)
# - CMD: Default arguments, easily overridden
#
# We use ENTRYPOINT because we always want to run the JAR.
# ---------------------------------------------------------------------
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]

# ╔══════════════════════════════════════════════════════════════════════════════════════════╗
# ║                                                                                           ║
# ║  STEP 11a COMPLETE!                                                                       ║
# ║  NEXT: docker-compose.yml updates (Step 11b)                                              ║
# ║                                                                                           ║
# ╚══════════════════════════════════════════════════════════════════════════════════════════╝
