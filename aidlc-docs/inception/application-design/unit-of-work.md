# Units of Work — Fintech Microservices Platform

## Development Order
Theo dependency order: Infrastructure → Account Service → Transfer Service → Transaction History Service

---

## Unit 0: Infrastructure & Common (Foundation)

**Type**: Foundation — phải hoàn thành trước tất cả business units
**Maven Modules**: `common`, `discovery-server`, `api-gateway`
**Scope**: Không có user stories riêng — hỗ trợ tất cả business services

### Common Module
**Responsibilities**:
- Shared domain events (TransferInitiatedEvent, TransferCompletedEvent, TransferFailedEvent, AccountCreatedEvent, AccountDebitedEvent, AccountCreditedEvent, DebitFailedEvent)
- Shared DTOs cho inter-service communication (AccountInfoResponse, BalanceResponse)
- Common exceptions (BaseException, ResourceNotFoundException, AccessDeniedException, ValidationException)
- Base classes (BaseEntity, BaseValueObject, BaseDomainEvent)
- Common utilities (Money value object, DateRange)

### Discovery Server (Eureka)
**Responsibilities**: Service registration và discovery

### API Gateway (Spring Cloud Gateway)
**Responsibilities**: Routing, JWT validation filter, rate limiting, CORS

### Code Organization
```
common/
  src/main/java/com/fintech/common/
    event/           -- shared domain events
    dto/             -- shared DTOs
    exception/       -- common exceptions
    domain/          -- base classes (BaseEntity, BaseValueObject)
    util/            -- utilities (Money, DateRange)

discovery-server/
  src/main/java/com/fintech/discovery/
    DiscoveryServerApplication.java

api-gateway/
  src/main/java/com/fintech/gateway/
    ApiGatewayApplication.java
    config/          -- route config, security config
    filter/          -- JWT validation filter
```

---

## Unit 1: Account Service

**Type**: Business Microservice
**Bounded Context**: Account Management
**Maven Module**: `account-service`
**Database**: account-db (PostgreSQL)
**Dependencies**: common module

### Responsibilities
- User registration và authentication (JWT)
- Bank account lifecycle (create, get balance)
- Internal API cho Transfer Service (verify account, debit/credit)

### Stories Covered
- US-ACC-01: Đăng ký tài khoản người dùng
- US-ACC-02: Đăng nhập
- US-ACC-03: Tạo tài khoản ngân hàng
- US-ACC-04: Xem số dư tài khoản

### Code Organization (Hexagonal)
```
account-service/
  src/main/java/com/fintech/account/
    domain/
      model/         -- User, BankAccount (aggregates), Value Objects
      event/         -- domain-specific events
      port/
        in/          -- inbound ports (use case interfaces)
        out/         -- outbound ports (repository, event publisher)
      service/       -- domain services
    application/
      service/       -- use case implementations
      dto/           -- request/response DTOs
    infrastructure/
      adapter/
        in/
          rest/      -- REST controllers
          kafka/     -- Kafka consumers (transfer events)
        out/
          persistence/ -- JPA entities, repositories
          messaging/   -- Kafka producer
      config/        -- Spring config, security config
      security/      -- JWT provider, security filters
  src/main/resources/
    application.yml
    db/migration/    -- Flyway migrations
  src/test/
    java/            -- unit tests, integration tests, PBT
```

---

## Unit 2: Transfer Service

**Type**: Business Microservice
**Bounded Context**: Fund Transfer
**Maven Module**: `transfer-service`
**Database**: transfer-db (PostgreSQL)
**Dependencies**: common module, Account Service (REST client)

### Responsibilities
- Fund transfer initiation và validation
- Transfer preview/confirmation
- Saga orchestration (choreography via Kafka)
- Business rules enforcement (daily limit, minimum amount, etc.)

### Stories Covered
- US-TRF-01: Chuyển tiền nội bộ
- US-TRF-02: Xác nhận giao dịch trước khi chuyển

### Code Organization (Hexagonal)
```
transfer-service/
  src/main/java/com/fintech/transfer/
    domain/
      model/         -- Transfer (aggregate), Value Objects
      event/         -- domain-specific events
      port/
        in/          -- inbound ports
        out/         -- outbound ports (repo, event publisher, account service port)
      service/       -- TransferValidationService
    application/
      service/       -- use case implementations
      dto/           -- request/response DTOs
    infrastructure/
      adapter/
        in/
          rest/      -- REST controllers
          kafka/     -- Kafka consumers (account events)
        out/
          persistence/ -- JPA entities, repositories
          messaging/   -- Kafka producer
          rest/        -- AccountServiceRestClient
      config/        -- Spring config
  src/main/resources/
    application.yml
    db/migration/    -- Flyway migrations
  src/test/
```

---

## Unit 3: Transaction History Service

**Type**: Business Microservice
**Bounded Context**: Transaction History
**Maven Module**: `transaction-history-service`
**Database**: history-db (PostgreSQL)
**Dependencies**: common module (events only — no REST dependency on other services)

### Responsibilities
- Transaction record storage (event-driven — consumes Kafka events)
- Transaction history query (paginated, filtered)
- Transaction detail retrieval

### Stories Covered
- US-TXH-01: Xem lịch sử giao dịch
- US-TXH-02: Lọc lịch sử giao dịch
- US-TXH-03: Xem chi tiết giao dịch

### Code Organization (Hexagonal)
```
transaction-history-service/
  src/main/java/com/fintech/history/
    domain/
      model/         -- TransactionRecord (aggregate), Value Objects
      port/
        in/          -- inbound ports
        out/         -- outbound ports (repository)
    application/
      service/       -- use case implementations
      dto/           -- request/response DTOs
    infrastructure/
      adapter/
        in/
          rest/      -- REST controllers
          kafka/     -- Kafka consumers (transfer events)
        out/
          persistence/ -- JPA entities, repositories
      config/        -- Spring config
  src/main/resources/
    application.yml
    db/migration/    -- Flyway migrations
  src/test/
```
