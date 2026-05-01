# Services & Orchestration — Fintech Microservices Platform

---

## Service Orchestration Patterns

### Pattern 1: Transfer Saga (Choreography-based)

Chuyển tiền nội bộ sử dụng choreography saga qua Kafka events:

```
Transfer Service                Account Service              Transaction History Service
      |                              |                              |
      | 1. InitiateTransfer          |                              |
      |---> Validate business rules  |                              |
      |---> Create Transfer (PENDING)|                              |
      |                              |                              |
      | 2. Publish TransferInitiatedEvent                           |
      |----------------------------->|                              |
      |                              | 3. Debit source account      |
      |                              | 4. Credit dest account       |
      |                              | 5. Publish AccountDebitedEvent|
      |<-----------------------------|                              |
      |                              |                              |
      | 6. Update Transfer (COMPLETED)                              |
      | 7. Publish TransferCompletedEvent                           |
      |----------------------------->|----------------------------->|
      |                              |                              |
      |                              |              8. Create DEBIT record
      |                              |              9. Create CREDIT record
```

**Compensation Flow (on failure):**
```
Transfer Service                Account Service
      |                              |
      | TransferInitiatedEvent       |
      |----------------------------->|
      |                              | Debit fails (insufficient balance)
      |                              | Publish DebitFailedEvent
      |<-----------------------------|
      |                              |
      | Update Transfer (FAILED)     |
      | Publish TransferFailedEvent  |
```

### Pattern 2: Query via REST (Synchronous)

Transfer Service cần verify account info trước khi initiate transfer:

```
Client --> API Gateway --> Transfer Service --REST--> Account Service
                               |
                               | GET /internal/accounts/{accountNumber}
                               | GET /internal/accounts/{accountNumber}/balance
```

Sử dụng internal REST endpoints (không expose qua API Gateway).

---

## Domain Services

### TransferValidationService (Transfer Service — Domain Layer)

Stateless domain service cho transfer validation:
- Validate minimum transfer amount (>= 1,000 VND)
- Validate source != destination
- Validate daily transfer limit
- Validate account status (ACTIVE)
- Validate sufficient balance

### JwtTokenService (Account Service — Infrastructure Layer)

- Generate access token (short-lived: 15 min)
- Generate refresh token (long-lived: 7 days)
- Validate and parse token
- Extract user claims

---

## Cross-Service Communication Summary

| From | To | Method | Purpose |
|---|---|---|---|
| API Gateway | Account Service | REST | Route auth + account requests |
| API Gateway | Transfer Service | REST | Route transfer requests |
| API Gateway | Transaction History Service | REST | Route history requests |
| Transfer Service | Account Service | REST (internal) | Verify account, check balance |
| Account Service | Kafka | Event publish | AccountCreatedEvent |
| Transfer Service | Kafka | Event publish | TransferInitiatedEvent, TransferCompletedEvent, TransferFailedEvent |
| Account Service | Kafka | Event consume | TransferInitiatedEvent (debit/credit) |
| Transaction History Service | Kafka | Event consume | TransferCompletedEvent, TransferFailedEvent (create records) |

---

## Kafka Topic Design

| Topic | Producer | Consumer(s) | Event Types |
|---|---|---|---|
| `account-events` | Account Service | Transfer Service | AccountCreatedEvent, AccountDebitedEvent, AccountCreditedEvent, DebitFailedEvent |
| `transfer-events` | Transfer Service | Account Service, Transaction History Service | TransferInitiatedEvent, TransferCompletedEvent, TransferFailedEvent |
