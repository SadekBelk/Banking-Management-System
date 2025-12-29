# Digital Banking Management - AI Coding Instructions

## Architecture Overview

This is a **microservices-based digital banking system** using Spring Boot 3.5+ with Java 21. Services communicate via **REST** for external APIs and **gRPC** for internal inter-service calls.

### Service Topology & Ports
| Service | HTTP Port | gRPC Port | Database | Role |
|---------|-----------|-----------|----------|------|
| customer-service | 4000 | - | customerdb:5432 | Customer management (REST only) |
| account-service | 4001 | 9001 | accountdb:5433 | Balance owner, **gRPC server** |
| transaction-service | 4002 | - | transactiondb:5434 | Ledger/audit trail |
| payment-service | 4003 | - | paymentdb:5435 | Payment orchestrator, **gRPC client** |

### Critical Data Flow: Payment Processing
```
payment-service (orchestrator) → gRPC → account-service (balance owner)
                                          ↓
                              ReserveBalance → CommitReservation
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
