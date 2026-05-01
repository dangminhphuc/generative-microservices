# Logical Components — Account Service

## Component Diagram

```
+---------------------------------------------------------------+
|                    Account Service                            |
|                                                               |
|  +-------------------------+  +---------------------------+   |
|  |   Inbound Adapters      |  |   Application Layer       |   |
|  |                         |  |                           |   |
|  |  UserController         |  |  RegisterUserUseCase      |   |
|  |  BankAccountController  |  |  LoginUseCase             |   |
|  |  AuthController         |  |  RefreshTokenUseCase      |   |
|  |  TransferEventConsumer  |  |  CreateBankAccountUseCase  |   |
|  |  (Kafka)                |  |  GetBalanceUseCase        |   |
|  |                         |  |  GetAccountByNumberUseCase|   |
|  +-------------------------+  |  DebitCreditUseCase       |   |
|                               +---------------------------+   |
|                                                               |
|  +-------------------------+  +---------------------------+   |
|  |   Domain Layer          |  |   Outbound Adapters       |   |
|  |                         |  |                           |   |
|  |  User (Aggregate)       |  |  UserJpaRepository        |   |
|  |  BankAccount (Aggregate)|  |  BankAccountJpaRepository |   |
|  |  Value Objects:         |  |  KafkaEventPublisher      |   |
|  |    Email, PhoneNumber,  |  |  BCryptPasswordEncoder    |   |
|  |    HashedPassword,      |  |                           |   |
|  |    AccountNumber, Money |  +---------------------------+   |
|  |  AccountNumberGenerator |                                  |
|  +-------------------------+                                  |
|                                                               |
|  +-------------------------+  +---------------------------+   |
|  |   Cross-Cutting         |  |   Configuration           |   |
|  |                         |  |                           |   |
|  |  GlobalExceptionHandler |  |  SecurityConfig           |   |
|  |  CorrelationIdFilter    |  |  KafkaConfig              |   |
|  |  RequestLoggingFilter   |  |  JpaConfig                |   |
|  |                         |  |  CacheConfig              |   |
|  |                         |  |  JwtTokenService          |   |
|  +-------------------------+  +---------------------------+   |
+---------------------------------------------------------------+
         |              |              |
         v              v              v
   +-----------+  +-----------+  +-----------+
   | account   |  |   Kafka   |  |  Eureka   |
   |    db     |  |  Broker   |  |  Server   |
   +-----------+  +-----------+  +-----------+
```

## Component Inventory

### Inbound Adapters (REST Controllers)

| Component | Endpoints | Auth |
|---|---|---|
| AuthController | POST /auth/register, POST /auth/login, POST /auth/refresh | Public |
| BankAccountController | POST /accounts, GET /accounts, GET /accounts/{id} | JWT required |
| InternalAccountController | GET /internal/accounts/{number}, POST /internal/accounts/{number}/debit, POST /internal/accounts/{number}/credit | Internal (service-to-service) |

### Inbound Adapters (Kafka Consumers)

| Component | Topic | Events Consumed |
|---|---|---|
| TransferEventConsumer | transfer-events | TransferInitiatedEvent |

### Outbound Adapters

| Component | Target | Purpose |
|---|---|---|
| UserJpaRepository | account-db | User CRUD |
| BankAccountJpaRepository | account-db | BankAccount CRUD |
| KafkaEventPublisher | Kafka | Publish domain events to account-events topic |
| BCryptPasswordEncoder | — | Password hashing |

### Cross-Cutting Components

| Component | Purpose | SECURITY Rule |
|---|---|---|
| GlobalExceptionHandler | Catch all exceptions, return safe error responses | SECURITY-09, SECURITY-15 |
| CorrelationIdFilter | Extract/generate X-Correlation-Id, set MDC | SECURITY-03 |
| RequestLoggingFilter | Log request/response metadata (no body/PII) | SECURITY-03 |
| JwtTokenService | Generate/validate JWT tokens | SECURITY-12 |

### Configuration Components

| Component | Purpose |
|---|---|
| SecurityConfig | Spring Security filter chain, public/protected endpoints |
| KafkaConfig | Kafka producer/consumer configuration, topic creation |
| JpaConfig | JPA auditing (createdAt, updatedAt auto-fill) |
| FlywayConfig | Database migration configuration |
| CacheConfig | Caffeine cache manager, cache definitions (users-by-email, accounts-by-number, accounts-by-userId) |
