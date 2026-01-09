# Digital Banking Management - AI Coding Instructions

## Architecture Overview

This is a **microservices-based digital banking system** using Spring Boot 3.5+ with Java 21. Services communicate via **REST** for external APIs and **gRPC** for internal inter-service calls.

### Service Topology & Ports
| Service | HTTP Port | gRPC Port | Database | Role |
|---------|-----------|-----------|----------|------|
| customer-service | 4000 | - | customerdb:5432 | Customer management (REST only) |
| account-service | 4001 | 9001 | accountdb:5433 | Balance owner, **gRPC server** |
| transaction-service | 4002 | 9002 | transactiondb:5434 | Ledger/audit trail, **gRPC server** |
| payment-service | 4003 | - | paymentdb:5435 | Payment orchestrator, **gRPC client** |
| auth-service | 4005 | - | authdb:5436 | JWT authentication, token management |

### Critical Data Flow: Payment Processing (5-Step gRPC Flow)
```
payment-service (orchestrator)
    │
    ├─ Step 1 → gRPC → account-service.ReserveBalance()
    │                  (holds funds, returns reservationId)
    │
    ├─ Step 2 → gRPC → transaction-service.CreateTransaction()
    │                  (creates PENDING ledger entry, returns transactionId)
    │
    ├─ Step 3 → gRPC → account-service.CreditBalance()
    │                  (adds funds to destination)
    │
    ├─ Step 4 → gRPC → account-service.CommitReservation()
    │                  (permanently deducts from source)
    │
    └─ Step 5 → gRPC → transaction-service.CompleteTransaction()
                       (marks transaction as COMPLETED)
```
The reservation pattern prevents double-spending: `ReserveBalance` → `CommitReservation` OR `ReleaseReservation`.

## Project Structure Conventions

### Package Layout (per service)
```
com.bankingmanagement.{service}/
├── controller/    # REST endpoints
├── dto/           # Request/Response DTOs
├── exception/     # Domain exceptions + GlobalExceptionHandler
├── grpc/          # gRPC service/client implementations
├── mapper/        # MapStruct mappers (see pattern below)
├── model/         # JPA entities
├── repository/    # Spring Data JPA repositories
├── service/       # Interface + impl/ subdirectory
└── client/        # HTTP clients to other services (WebClient)
```

### Shared Proto Module
The `banking-proto` module contains all `.proto` definitions. **Always build this first**:
```bash
cd banking-proto && mvn clean install
```
Proto files generate to: `com.banking.proto.{domain}` (e.g., `com.banking.proto.account`)

## Key Patterns & Conventions

### MapStruct Mappers
Always use `componentModel = "spring"`. For update operations, ignore immutable fields:
```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(CustomerRequestDto dto, @MappingTarget Customer customer);
}
```

### Exception Handling
- REST: Use `GlobalExceptionHandler` with `ApiError` records containing `ApiErrorCode` enum
- gRPC: Translate domain exceptions to `io.grpc.Status` codes (NOT_FOUND, INVALID_ARGUMENT, etc.)
```java
// gRPC error example (AccountGrpcService.java)
responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
```

### gRPC Integration
- **Server** (account-service): Use `@GrpcService` annotation, extend `*ImplBase`
- **Client** (payment-service): Use `@GrpcClient("service-name")` to inject stub
- Channel config in `application.yml`: `grpc.client.account-service.address: static://account-service:9001`

### Money Handling
**Never use `double` for currency.** Use `BigDecimal` in Java, `int64 amount` (cents) + `string currency` in proto:
```protobuf
message Money {
  string currency = 1;
  int64 amount = 2;  // Minor units (cents)
}
```

## Build & Run Commands

### Local Development (H2 in-memory)
```bash
# Build proto first
cd banking-proto && mvn clean install

# Run single service with local profile
cd account-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Docker Compose Profiles
```bash
docker compose --profile grpc up --build    # account + payment + their DBs
docker compose --profile full up --build    # All services
docker compose down -v                       # Stop + remove volumes
```

### Running Tests
```bash
cd account-service && mvn test              # Unit + integration tests
```
gRPC integration tests use `@ActiveProfiles("local")` with H2.

## Configuration Files

- `application.yml` - Docker/production config (PostgreSQL, Docker service names)
- `application-local.yml` - Local dev config (H2, localhost URLs, H2 console at `/h2-console`)

Environment variable overrides: `SPRING_DATASOURCE_URL`, `GRPC_SERVER_PORT`, etc.

## API Testing

HTTP request files in `api-request/` directory. Key integration test:
- [grpc-integration-test.http](api-request/integration-test/grpc-integration-test.http) - Full payment flow

## Dependencies to Know

- **grpc-server-spring-boot-starter** / **grpc-client-spring-boot-starter** (`net.devh`) - gRPC auto-config
- **MapStruct** 1.5.5 - DTO mapping (requires `lombok-mapstruct-binding` in annotation processors)
- **springdoc-openapi** 2.8+ - Swagger UI at `/swagger-ui.html`
- Java 21 preview features enabled (`--enable-preview` in compiler args)

## Common Pitfalls

1. **Proto changes**: Always rebuild `banking-proto` AND dependent services
2. **Docker networking**: Services reference each other by container name (e.g., `account-service:9001`), not `localhost`
3. **Lombok + MapStruct order**: Annotation processor order matters - see `pom.xml` for correct setup
4. **gRPC port vs HTTP port**: account-service exposes BOTH 4001 (REST) and 9001 (gRPC)

## Implementation Decisions & Lessons Learned

### gRPC Dependency Management (CRITICAL)
**DO NOT** explicitly specify gRPC library versions in service `pom.xml` files. Let `grpc-server-spring-boot-starter` manage transitive dependencies.

❌ **Wrong** (causes `ClassNotFoundException: io.grpc.InternalConfiguratorRegistry`):
```xml
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-server-spring-boot-starter</artifactId>
    <version>3.1.0.RELEASE</version>
</dependency>
<!-- DON'T add these - they conflict with the starter -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.69.0</version>
</dependency>
```

✅ **Correct** (let starter manage versions):
```xml
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-server-spring-boot-starter</artifactId>
    <version>3.1.0.RELEASE</version>
</dependency>
<!-- No explicit grpc-* dependencies needed -->
```

### banking-proto Module Version
The `banking-proto` module uses version `1.0.0`. All services must reference this exact version:
```xml
<dependency>
    <groupId>com.BankingManagement</groupId>
    <artifactId>banking-proto</artifactId>
    <version>1.0.0</version>
</dependency>
```

### UUID-Based Entity IDs
All entities use UUID primary keys with JPA auto-generation:
```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```
Proto messages use `string` for IDs, convert with `UUID.fromString()` / `uuid.toString()`.

### Timestamp Handling
- **Java entities**: Use `java.time.Instant` (not `LocalDateTime`)
- **Proto messages**: Use `google.protobuf.Timestamp`
- **Conversion**: `Timestamps.fromMillis(instant.toEpochMilli())` and `Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos())`

### gRPC Client Configuration Pattern
In `application.yml` for gRPC clients:
```yaml
grpc:
  client:
    account-service:
      address: static://account-service:9001    # Docker
      negotiationType: plaintext
    transaction-service:
      address: static://transaction-service:9002
      negotiationType: plaintext
```
For local development, use `static://localhost:900X`.

### Rollback Handling in Payment Flow
If any step fails after reservation, the `handleRollback()` method:
1. Releases the reservation via `account-service.ReleaseReservation()`
2. Fails the transaction via `transaction-service.FailTransaction()` (if created)
3. Updates payment status to `FAILED` with reason

### Proto File Organization
Proto files in `banking-proto/src/main/proto/`:
- `account.proto` - ReserveBalance, CreditBalance, CommitReservation, ReleaseReservation
- `transaction.proto` - CreateTransaction, CompleteTransaction, FailTransaction, GetTransaction

Each proto uses package `bank.{domain}` and `java_package = "com.banking.proto.{domain}"`.

## Kafka Event-Driven Architecture

### Overview
The system uses **Apache Kafka** for asynchronous event publishing. Services publish domain events after state changes, enabling:
- Audit trail and compliance logging
- Future event sourcing capabilities
- Decoupled notification/analytics services

### Kafka Infrastructure
```yaml
# Docker Compose profile: kafka, grpc, full
kafka:        # Apache Kafka 3.7+ with KRaft (no Zookeeper)
  port: 9092  # External (host machine)
  port: 9093  # Internal (Docker network)
kafka-ui:     # Provectus Kafka UI for debugging
  port: 8090  # http://localhost:8090
```

### Topics
| Topic | Producer | Events |
|-------|----------|--------|
| `banking.transactions.events` | transaction-service | TRANSACTION_CREATED, TRANSACTION_COMPLETED, TRANSACTION_FAILED |
| `banking.payments.events` | payment-service | PAYMENT_INITIATED, PAYMENT_PROCESSING, PAYMENT_COMPLETED, PAYMENT_FAILED, PAYMENT_CANCELLED |

### Event Structure
All events follow a consistent structure:
```java
{
  "eventId": "uuid",           // Unique event ID
  "eventType": "EVENT_NAME",   // e.g., PAYMENT_COMPLETED
  "eventTimestamp": "instant", // When event occurred
  "eventVersion": "1.0",       // Schema version
  // ... domain-specific fields
}
```

### Partitioning Strategy
- **Transaction events**: Partitioned by `transactionId` (ordering per transaction)
- **Payment events**: Partitioned by `paymentId` (ordering per payment)

### Key Classes
| Service | Class | Purpose |
|---------|-------|---------|
| transaction-service | `TransactionEvent` | Event DTO |
| transaction-service | `TransactionEventPublisher` | Publishes to Kafka |
| transaction-service | `KafkaConfig` | Topic configuration |
| payment-service | `PaymentEvent` | Event DTO |
| payment-service | `PaymentEventPublisher` | Publishes to Kafka |
| payment-service | `KafkaConfig` | Topic configuration |

### Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9093}
    producer:
      acks: all              # Wait for all replicas
      retries: 3             # Retry on failure
      properties:
        enable.idempotence: true  # Exactly-once semantics
```

For local development, use `localhost:9092`.

## JWT Authentication (auth-service)

### Overview
The `auth-service` provides JWT-based authentication for the banking system. It handles user registration, login, token management, and validation.

### Endpoints
| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/auth/register` | POST | Register new user | No |
| `/api/auth/login` | POST | Login, get JWT tokens | No |
| `/api/auth/refresh` | POST | Refresh access token | No |
| `/api/auth/logout` | POST | Revoke refresh token | No |
| `/api/auth/logout-all` | POST | Logout all devices | Yes |
| `/api/auth/me` | GET | Get current user info | Yes |
| `/api/auth/change-password` | POST | Change password | Yes |
| `/api/auth/validate` | GET | Validate token | No |

### Token Structure
- **Access Token**: Short-lived JWT (24h default), contains user ID, username, email, roles
- **Refresh Token**: Long-lived random string (7 days default), stored in database

### Roles
| Role | Description |
|------|-------------|
| `ROLE_USER` | Basic authenticated user |
| `ROLE_CUSTOMER` | Bank customer with account access |
| `ROLE_TELLER` | Bank teller with transaction capabilities |
| `ROLE_MANAGER` | Branch manager with elevated permissions |
| `ROLE_ADMIN` | System administrator with full access |

### Key Classes
| Class | Purpose |
|-------|---------|
| `JwtTokenProvider` | Generate/validate JWT tokens |
| `JwtAuthenticationFilter` | Extract & validate tokens on requests |
| `CustomUserDetailsService` | Load user from database |
| `SecurityConfig` | Spring Security configuration |

### Configuration
```yaml
jwt:
  secret: ${JWT_SECRET:base64EncodedSecret}  # Base64 encoded, min 256 bits
  expiration: 86400000                        # 24 hours
  refresh-expiration: 604800000               # 7 days
  issuer: digital-banking-auth-service
```

### Docker Profiles
```bash
docker compose --profile auth up --build     # Auth service only
docker compose --profile full up --build     # All services including auth
```

## Version History

| Date | Change | Impact |
|------|--------|--------|
| Jan 2026 | Added auth-service with JWT authentication | New service for user auth, token management |
| Dec 2025 | Added Kafka event publishing | transaction-service and payment-service now publish domain events to Kafka |
| Dec 2025 | Added transaction-service as gRPC server | payment-service now calls both account-service AND transaction-service via gRPC |
| Dec 2025 | Implemented 5-step payment flow | Full reservation + transaction ledger pattern |
| Dec 2025 | Fixed gRPC version conflicts | Removed explicit grpc-* deps, use starter's transitive deps |
