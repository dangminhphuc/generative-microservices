# Code Summary — Unit 1: Account Service

## Files Generated (~50 files)

### Project Setup (4 files)
- `account-service/pom.xml`
- `account-service/src/.../AccountServiceApplication.java`
- `account-service/src/main/resources/application.yml`
- `account-service/src/main/resources/db/migration/` (2 SQL files)

### Domain Layer (13 files)
- Value Objects: `Email`, `PhoneNumber`, `AccountNumber`, `UserStatus`, `AccountStatus`
- Aggregates: `User`, `BankAccount`
- Domain Service: `AccountNumberGenerator`
- Inbound Ports: `RegisterUserUseCase`, `LoginUseCase`, `RefreshTokenUseCase`, `CreateBankAccountUseCase`, `GetBalanceUseCase`, `GetAccountByNumberUseCase`, `DebitCreditUseCase`
- Outbound Ports: `UserRepository`, `BankAccountRepository`, `EventPublisher`, `PasswordEncoder`

### Application Layer (11 files)
- DTOs: `RegisterUserCommand`, `RegisterUserResponse`, `LoginCommand`, `LoginResponse`, `RefreshTokenCommand`, `CreateBankAccountCommand`, `CreateBankAccountResponse`
- Services: `RegisterUserService`, `AuthenticationService`, `BankAccountService`, `DebitCreditService`

### Infrastructure — Persistence (6 files)
- JPA Entities: `UserJpaEntity`, `BankAccountJpaEntity`
- JPA Repositories: `UserJpaRepository`, `BankAccountJpaRepository`
- Adapters: `UserRepositoryAdapter`, `BankAccountRepositoryAdapter`

### Infrastructure — Messaging (3 files)
- `KafkaEventPublisher`, `TransferEventConsumer`, `KafkaConfig`

### Infrastructure — REST (4 files)
- `AuthController`, `BankAccountController`, `InternalAccountController`, `GlobalExceptionHandler`

### Infrastructure — Security & Config (5 files)
- `JwtTokenService`, `SecurityConfig`, `CorrelationIdFilter`, `JpaAuditingConfig`, `BeanConfig`

## Story Coverage
- ✅ US-ACC-01: Register User
- ✅ US-ACC-02: Login
- ✅ US-ACC-03: Create Bank Account
- ✅ US-ACC-04: Get Balance
