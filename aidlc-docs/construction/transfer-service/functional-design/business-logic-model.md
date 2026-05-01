# Business Logic Model — Transfer Service

## Use Case Flows

### UC-01: Preview Transfer

```
Input: PreviewTransferCommand(userId, sourceAccountNumber, destAccountNumber, amount, description)

1. Validate minimum amount (>= 1,000 VND)
2. Validate source != destination
3. Call Account Service REST: GET /internal/accounts/{sourceAccountNumber}
   → Validate exists, status ACTIVE, belongs to userId
4. Call Account Service REST: GET /internal/accounts/{destAccountNumber}
   → Validate exists
5. Check daily limit: sum today's transfers for source + new amount <= 500M VND
6. Return TransferPreviewResponse(sourceAccountName, destAccountName masked, amount, description)
```

### UC-02: Initiate Transfer

```
Input: InitiateTransferCommand(userId, sourceAccountNumber, destAccountNumber, amount, description)

1. Validate minimum amount (>= 1,000 VND)
2. Validate source != destination
3. Call Account Service REST: verify source account (exists, ACTIVE, belongs to userId)
4. Call Account Service REST: verify dest account (exists)
5. Call Account Service REST: verify source balance >= amount
6. Check daily limit
7. Create Transfer aggregate (status = PENDING)
8. Save Transfer via TransferRepository
9. Publish TransferInitiatedEvent via Kafka
10. Return TransferResponse(transferId, status=PENDING, amount, createdAt)
```

### UC-03: Handle Account Events (Kafka Consumer)

```
On AccountDebitedEvent:
  1. Find Transfer by transferId
  2. (Debit confirmed — wait for credit confirmation or handle in same flow)

On AccountCreditedEvent:
  1. Find Transfer by transferId
  2. Mark Transfer as COMPLETED, set completedAt
  3. Save Transfer
  4. Publish TransferCompletedEvent

On DebitFailedEvent:
  1. Find Transfer by transferId
  2. Mark Transfer as FAILED, set reason
  3. Save Transfer
  4. Publish TransferFailedEvent
```

## Outbound REST Calls (to Account Service)

| Endpoint | Method | Purpose |
|---|---|---|
| /internal/accounts/{number} | GET | Verify account exists, get owner name, status |

Note: Balance check is done via the Account Service's debit operation (fail-fast on insufficient balance via DebitFailedEvent).
