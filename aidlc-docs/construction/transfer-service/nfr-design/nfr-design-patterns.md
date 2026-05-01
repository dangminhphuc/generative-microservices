# NFR Design Patterns — Transfer Service

Inherits all patterns from Account Service. Additional patterns:

## 1. Outbound REST Client Pattern

```java
// AccountServiceClient — implements AccountServicePort
// Uses Spring RestClient with timeout and retry
RestClient restClient = RestClient.builder()
    .baseUrl("http://account-service")  // Eureka service name
    .build();

// Timeout: 5 seconds
// Retry: 3 attempts with exponential backoff on 5xx errors
```

## 2. Saga Choreography Pattern

```
Transfer Service publishes TransferInitiatedEvent
  → Account Service consumes, performs debit+credit
  → Account Service publishes AccountDebitedEvent/AccountCreditedEvent or DebitFailedEvent
  → Transfer Service consumes, updates Transfer status
  → Transfer Service publishes TransferCompletedEvent or TransferFailedEvent
  → Transaction History Service consumes, creates records
```

## 3. Daily Limit Tracking

```sql
SELECT COALESCE(SUM(amount), 0)
FROM transfers
WHERE source_account_number = ?
  AND status IN ('PENDING', 'COMPLETED')
  AND created_at >= ? -- start of today
  AND created_at < ?  -- start of tomorrow
```

## 4. Idempotent Event Processing

Transfer events consumed with transferId as idempotency key.
Check if transfer already in terminal state (COMPLETED/FAILED) before processing.
