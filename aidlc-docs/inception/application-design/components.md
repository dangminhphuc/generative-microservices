# Components — Fintech Microservices Platform

## Overview

Hệ thống gồm 5 components chính: 3 business microservices + 2 infrastructure services.

---

## Component 1: Account Service

**Bounded Context**: Account Management
**Responsibility**: Quản lý user registration, authentication, và bank account lifecycle.

### Hexagonal Layers

**Domain Layer:**
- User (Aggregate Root) — identity, credentials, profile
- BankAccount (Aggregate Root) — account number, balance, status
- Value Objects: Email, PhoneNumber, Password, AccountNumber, Money, AccountStatus
- Domain Events: UserRegisteredEvent, AccountCreatedEvent
- Repository Ports: UserRepository, BankAccountRepository

**Application Layer (Ports):**
- Inbound Ports: RegisterUserUseCase, LoginUseCase, CreateBankAccountUseCase, GetBalanceUseCase
- Outbound Ports: UserRepository, BankAccountRepository, EventPublisher, PasswordEncoder

**Infrastructure Layer (Adapters):**
- Inbound Adapters: REST Controller (UserController, BankAccountController)
- Outbound Adapters: JPA Repository (UserJpaRepository, BankAccountJpaRepository), KafkaEventPublisher
- Security: JWT Token Provider, Spring Security Configuration

---

## Component 2: Transfer Service

**Bounded Context**: Fund Transfer
**Responsibility**: Xử lý chuyển tiền nội bộ, validation business rules, saga orchestration.

### Hexagonal Layers

**Domain Layer:**
- Transfer (Aggregate Root) — source, destination, amount, status, timestamps
- Value Objects: Money, TransferStatus, AccountReference
- Domain Events: TransferInitiatedEvent, TransferCompletedEvent, TransferFailedEvent
- Domain Services: TransferValidationService (daily limit, minimum amount, account status)
- Repository Ports: TransferRepository

**Application Layer (Ports):**
- Inbound Ports: InitiateTransferUseCase, PreviewTransferUseCase
- Outbound Ports: TransferRepository, EventPublisher, AccountServicePort (REST client to verify accounts/balances)

**Infrastructure Layer (Adapters):**
- Inbound Adapters: REST Controller (TransferController), Kafka Consumer (for account events)
- Outbound Adapters: JPA Repository (TransferJpaRepository), KafkaEventPublisher, AccountServiceRestClient

---

## Component 3: Transaction History Service

**Bounded Context**: Transaction History
**Responsibility**: Lưu trữ và truy vấn lịch sử giao dịch. Event consumer — nhận events từ Transfer Service.

### Hexagonal Layers

**Domain Layer:**
- TransactionRecord (Aggregate Root) — transactionId, type, amount, balanceBefore, balanceAfter, counterparty, description, timestamp
- Value Objects: TransactionType (CREDIT/DEBIT), DateRange
- Repository Ports: TransactionRecordRepository

**Application Layer (Ports):**
- Inbound Ports: GetTransactionHistoryUseCase, GetTransactionDetailUseCase, FilterTransactionsUseCase
- Outbound Ports: TransactionRecordRepository

**Infrastructure Layer (Adapters):**
- Inbound Adapters: REST Controller (TransactionHistoryController), Kafka Consumer (TransferCompletedEvent, TransferFailedEvent)
- Outbound Adapters: JPA Repository (TransactionRecordJpaRepository)

---

## Component 4: API Gateway

**Type**: Infrastructure Service
**Technology**: Spring Cloud Gateway
**Responsibility**: Single entry point, routing, JWT validation, rate limiting, CORS.

### Routing Rules
- `/api/accounts/**` → Account Service
- `/api/transfers/**` → Transfer Service
- `/api/transactions/**` → Transaction History Service
- `/api/auth/**` → Account Service (login/register — public endpoints)

---

## Component 5: Discovery Server

**Type**: Infrastructure Service
**Technology**: Spring Cloud Netflix Eureka
**Responsibility**: Service registration và discovery.
