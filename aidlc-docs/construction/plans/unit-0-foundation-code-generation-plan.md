# Code Generation Plan — Unit 0: Foundation

## Unit Context
- **Unit**: Foundation (Common Module + Discovery Server + API Gateway)
- **Type**: Infrastructure — no business logic, no user stories
- **Purpose**: Thiết lập project structure, shared libraries, và infrastructure services
- **Dependencies**: None (foundation cho tất cả business units)

---

## Generation Steps

### Phase A: Project Structure & Parent POM

- [x] **Step 1**: Tạo parent POM (`pom.xml`) — Maven multi-module project
  - groupId: `com.fintech`
  - artifactId: `fintech-platform`
  - Java 21, Spring Boot 3.x parent
  - 6 modules: common, discovery-server, api-gateway, account-service, transfer-service, transaction-history-service
  - Dependency management: Spring Cloud, Kafka, PostgreSQL, jqwik, Flyway

### Phase B: Common Module

- [ ] **Step 2**: Tạo common module POM (`common/pom.xml`)
- [x] **Step 3**: Tạo base domain classes
  - `BaseEntity` — abstract class với id, createdAt, updatedAt
  - `BaseValueObject` — abstract class cho value object equality
  - `BaseDomainEvent` — abstract class cho domain events (eventId, occurredAt, aggregateId)
- [x] **Step 4**: Tạo shared domain events
  - Account events: `AccountCreatedEvent`, `AccountDebitedEvent`, `AccountCreditedEvent`, `DebitFailedEvent`
  - Transfer events: `TransferInitiatedEvent`, `TransferCompletedEvent`, `TransferFailedEvent`
- [x] **Step 5**: Tạo shared DTOs
  - `AccountInfoResponse`, `BalanceResponse`
- [x] **Step 6**: Tạo common exceptions
  - `BaseException`, `ResourceNotFoundException`, `AccessDeniedException`, `ValidationException`, `BusinessRuleException`
- [x] **Step 7**: Tạo Money value object (amount + currency, immutable, arithmetic operations)

### Phase C: Discovery Server (Eureka)

- [x] **Step 8**: Tạo discovery-server module POM (`discovery-server/pom.xml`)
- [x] **Step 9**: Tạo `DiscoveryServerApplication.java` + `application.yml`
  - `@EnableEurekaServer`
  - Port: 8761

### Phase D: API Gateway (Spring Cloud Gateway)

- [x] **Step 10**: Tạo api-gateway module POM (`api-gateway/pom.xml`)
- [x] **Step 11**: Tạo `ApiGatewayApplication.java` + `application.yml`
  - Route config: `/api/auth/**`, `/api/accounts/**`, `/api/transfers/**`, `/api/transactions/**`
  - Eureka client registration
- [x] **Step 12**: Tạo JWT Authentication Filter
  - `JwtAuthenticationFilter` — validate JWT token trên mỗi request (trừ public endpoints)
  - Extract claims, forward user info to downstream services via headers
- [x] **Step 13**: Tạo Gateway config classes
  - `RouteConfig` — route definitions
  - `SecurityConfig` — CORS, public endpoints whitelist
  - `RateLimitConfig` — basic rate limiting

### Phase E: Docker Compose & Documentation

- [x] **Step 14**: Tạo `docker-compose.yml`
  - PostgreSQL (3 databases: account-db, transfer-db, history-db)
  - Apache Kafka + Zookeeper
  - Prometheus + Grafana (basic config)
- [x] **Step 15**: Tạo `Dockerfile` template cho mỗi service (multi-stage build)
- [x] **Step 16**: Tạo root `README.md` — project overview, setup instructions, module descriptions
- [x] **Step 17**: Tạo code summary documentation (`aidlc-docs/construction/unit-0-foundation/code/code-summary.md`)

---

## Files to Generate

| # | File Path | Description |
|---|---|---|
| 1 | `pom.xml` | Parent POM |
| 2 | `common/pom.xml` | Common module POM |
| 3 | `common/src/.../domain/BaseEntity.java` | Base entity |
| 4 | `common/src/.../domain/BaseValueObject.java` | Base value object |
| 5 | `common/src/.../event/BaseDomainEvent.java` | Base domain event |
| 6 | `common/src/.../event/account/*.java` | Account events (4 files) |
| 7 | `common/src/.../event/transfer/*.java` | Transfer events (3 files) |
| 8 | `common/src/.../dto/AccountInfoResponse.java` | Shared DTO |
| 9 | `common/src/.../dto/BalanceResponse.java` | Shared DTO |
| 10 | `common/src/.../exception/*.java` | Common exceptions (5 files) |
| 11 | `common/src/.../domain/Money.java` | Money value object |
| 12 | `discovery-server/pom.xml` | Eureka POM |
| 13 | `discovery-server/src/.../DiscoveryServerApplication.java` | Eureka main |
| 14 | `discovery-server/src/main/resources/application.yml` | Eureka config |
| 15 | `api-gateway/pom.xml` | Gateway POM |
| 16 | `api-gateway/src/.../ApiGatewayApplication.java` | Gateway main |
| 17 | `api-gateway/src/.../filter/JwtAuthenticationFilter.java` | JWT filter |
| 18 | `api-gateway/src/.../config/RouteConfig.java` | Route definitions |
| 19 | `api-gateway/src/.../config/SecurityConfig.java` | Security config |
| 20 | `api-gateway/src/.../config/RateLimitConfig.java` | Rate limiting |
| 21 | `api-gateway/src/main/resources/application.yml` | Gateway config |
| 22 | `docker-compose.yml` | Docker Compose |
| 23 | `*/Dockerfile` | Dockerfiles (3 services + gateway + discovery) |
| 24 | `README.md` | Project README |

**Estimated**: ~30 files
