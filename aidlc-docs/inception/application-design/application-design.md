# Application Design — Fintech Microservices Platform (Consolidated)

## 1. System Overview

Hệ thống Fintech/Banking gồm 5 components:
- **3 Business Microservices**: Account Service, Transfer Service, Transaction History Service
- **2 Infrastructure Services**: API Gateway (Spring Cloud Gateway), Discovery Server (Eureka)

Mỗi business service áp dụng Hexagonal Architecture (Ports & Adapters) với Tactical DDD.

---

## 2. Bounded Context Map

```
+-------------------+     REST (internal)     +-------------------+
|  Account Service  |<------------------------|  Transfer Service |
|  (Account Mgmt)   |                         |  (Fund Transfer)  |
+-------------------+                         +-------------------+
        |                                             |
        | Kafka: account-events          Kafka: transfer-events
        v                                             v
+---------------------------------------------------------------+
|                      Apache Kafka                             |
+---------------------------------------------------------------+
        |                                             |
        v                                             v
+-------------------+                         +-------------------+
| (consume account  |                         | Transaction Hist  |
|  events if needed)|                         | Service (consume  |
|                   |                         | transfer events)  |
+-------------------+                         +-------------------+
```

**Relationships:**
- Account ↔ Transfer: Upstream-Downstream (Account is upstream, Transfer depends on Account for validation)
- Transfer → Transaction History: Publisher-Subscriber (Transfer publishes events, History consumes)
- Account → Transaction History: No direct dependency

---

## 3. Component Summary

| Service | Bounded Context | Aggregates | Key Use Cases | Database |
|---|---|---|---|---|
| Account Service | Account Management | User, BankAccount | Register, Login, Create Account, Get Balance | account-db (PostgreSQL) |
| Transfer Service | Fund Transfer | Transfer | Preview Transfer, Initiate Transfer | transfer-db (PostgreSQL) |
| Transaction History Service | Transaction History | TransactionRecord | Get History, Filter, Get Detail | history-db (PostgreSQL) |

---

## 4. Communication Patterns

- **Client → Services**: REST via API Gateway (Spring Cloud Gateway)
- **Transfer → Account**: REST (internal, synchronous) — verify account, check balance, debit/credit
- **Services → Kafka**: Domain events (asynchronous) — TransferInitiatedEvent, TransferCompletedEvent, etc.
- **Kafka → Services**: Event consumption — Account Service processes transfer events, Transaction History creates records

---

## 5. Saga: Fund Transfer Flow

1. Client → Transfer Service: InitiateTransfer (REST)
2. Transfer Service → Account Service: Verify accounts + balance (REST)
3. Transfer Service: Create Transfer (PENDING), publish TransferInitiatedEvent (Kafka)
4. Account Service: Consume event, debit source, credit destination, publish AccountDebitedEvent
5. Transfer Service: Consume event, update Transfer (COMPLETED), publish TransferCompletedEvent
6. Transaction History Service: Consume event, create DEBIT + CREDIT records

**Compensation**: If debit fails → DebitFailedEvent → Transfer marked FAILED → TransferFailedEvent

---

## 6. Maven Module Structure

Parent POM (fintech-platform) with 6 modules:
1. `common` — shared events, DTOs, exceptions
2. `discovery-server` — Eureka Server
3. `api-gateway` — Spring Cloud Gateway
4. `account-service` — Account bounded context
5. `transfer-service` — Transfer bounded context
6. `transaction-history-service` — Transaction History bounded context

---

## 7. Kafka Topics

| Topic | Producer | Consumers |
|---|---|---|
| `account-events` | Account Service | Transfer Service |
| `transfer-events` | Transfer Service | Account Service, Transaction History Service |

---

*Detailed artifacts: components.md, component-methods.md, services.md, component-dependency.md*
