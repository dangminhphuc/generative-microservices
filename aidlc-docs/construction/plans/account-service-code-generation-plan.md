# Code Generation Plan — Unit 1: Account Service

## Unit Context
- **Unit**: Account Service
- **Bounded Context**: Account Management
- **Stories**: US-ACC-01 (Register), US-ACC-02 (Login), US-ACC-03 (Create Account), US-ACC-04 (Get Balance)
- **Dependencies**: common module (already built in Unit 0)
- **Architecture**: Hexagonal (Ports & Adapters)

---

## Generation Steps

### Phase A: Project Setup

- [ ] **Step 1**: Tạo `account-service/pom.xml` với tất cả dependencies
- [ ] **Step 2**: Tạo `AccountServiceApplication.java` (main class)
- [ ] **Step 3**: Tạo `application.yml` + `application-docker.yml` (profiles)
- [ ] **Step 4**: Tạo Flyway migrations (V1 users, V2 bank_accounts, V3 indexes)

### Phase B: Domain Layer

- [ ] **Step 5**: Tạo Value Objects — `Email`, `PhoneNumber`, `HashedPassword`, `AccountNumber`, `AccountStatus`, `UserStatus`
- [ ] **Step 6**: Tạo `User` aggregate root
- [ ] **Step 7**: Tạo `BankAccount` aggregate root
- [ ] **Step 8**: Tạo Domain Service — `AccountNumberGenerator`
- [ ] **Step 9**: Tạo Inbound Ports (Use Case interfaces)
  - `RegisterUserUseCase`, `LoginUseCase`, `RefreshTokenUseCase`
  - `CreateBankAccountUseCase`, `GetBalanceUseCase`, `GetAccountByNumberUseCase`
  - `DebitCreditUseCase`
- [ ] **Step 10**: Tạo Outbound Ports (Repository + Service interfaces)
  - `UserRepository`, `BankAccountRepository`, `EventPublisher`, `PasswordEncoder`

### Phase C: Application Layer (Use Case Implementations)

- [ ] **Step 11**: Tạo Request/Response DTOs
- [ ] **Step 12**: Tạo `RegisterUserService` (implements RegisterUserUseCase) — US-ACC-01
- [ ] **Step 13**: Tạo `AuthenticationService` (implements LoginUseCase, RefreshTokenUseCase) — US-ACC-02
- [ ] **Step 14**: Tạo `BankAccountService` (implements CreateBankAccountUseCase, GetBalanceUseCase, GetAccountByNumberUseCase) — US-ACC-03, US-ACC-04
- [ ] **Step 15**: Tạo `DebitCreditService` (implements DebitCreditUseCase) — internal operation

### Phase D: Infrastructure Layer — Persistence Adapters

- [ ] **Step 16**: Tạo JPA Entities — `UserJpaEntity`, `BankAccountJpaEntity` (with mappers to/from domain)
- [ ] **Step 17**: Tạo JPA Repositories — `UserJpaRepository`, `BankAccountJpaRepository`
- [ ] **Step 18**: Tạo Repository Adapters — `UserRepositoryAdapter`, `BankAccountRepositoryAdapter` (implements domain ports)

### Phase E: Infrastructure Layer — Messaging Adapters

- [ ] **Step 19**: Tạo `KafkaEventPublisher` (implements EventPublisher port)
- [ ] **Step 20**: Tạo `TransferEventConsumer` (Kafka consumer for TransferInitiatedEvent)
- [ ] **Step 21**: Tạo Kafka configuration (`KafkaConfig`)

### Phase F: Infrastructure Layer — REST Adapters (Inbound)

- [ ] **Step 22**: Tạo `AuthController` (POST /auth/register, /auth/login, /auth/refresh) — US-ACC-01, US-ACC-02
- [ ] **Step 23**: Tạo `BankAccountController` (POST /accounts, GET /accounts) — US-ACC-03, US-ACC-04
- [ ] **Step 24**: Tạo `InternalAccountController` (GET /internal/accounts/{number}, POST debit/credit)

### Phase G: Infrastructure Layer — Security & Cross-Cutting

- [ ] **Step 25**: Tạo `JwtTokenService` (generate/validate JWT)
- [ ] **Step 26**: Tạo `SecurityConfig` (Spring Security filter chain)
- [ ] **Step 27**: Tạo `GlobalExceptionHandler` (@RestControllerAdvice)
- [ ] **Step 28**: Tạo `CorrelationIdFilter` (MDC-based correlation ID)

### Phase H: Configuration

- [ ] **Step 29**: Tạo `JpaAuditingConfig` (auto createdAt/updatedAt)
- [ ] **Step 30**: Tạo `BCryptPasswordEncoderAdapter` (implements PasswordEncoder port)

### Phase I: Documentation

- [ ] **Step 31**: Tạo code summary (`aidlc-docs/construction/account-service/code/code-summary.md`)

---

## Story Traceability

| Story | Steps |
|---|---|
| US-ACC-01 (Register) | 5, 6, 9, 10, 11, 12, 16-18, 22, 25-28 |
| US-ACC-02 (Login) | 5, 6, 9, 10, 11, 13, 16-18, 22, 25-28 |
| US-ACC-03 (Create Account) | 5, 7, 8, 9, 10, 11, 14, 16-19, 23, 27-28 |
| US-ACC-04 (Get Balance) | 7, 9, 10, 11, 14, 16-18, 23, 27-28 |

**Estimated**: ~50+ Java files
