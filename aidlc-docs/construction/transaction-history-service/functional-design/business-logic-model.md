# Business Logic Model — Transaction History Service

## Use Case Flows

### UC-01: Get Transaction History
```
Input: GetTransactionHistoryQuery(userId, accountNumber, page, size)
1. Verify account belongs to userId (via accountNumber ownership check)
2. Query TransactionRecordRepository with pagination
3. Return Page<TransactionRecordResponse>
```

### UC-02: Filter Transactions
```
Input: FilterTransactionsQuery(userId, accountNumber, startDate, endDate, type, page, size)
1. Validate startDate < endDate
2. Validate date range <= 12 months
3. Verify account belongs to userId
4. Query with filters (AND logic)
5. Return Page<TransactionRecordResponse>
```

### UC-03: Get Transaction Detail
```
Input: GetTransactionDetailQuery(userId, transactionId)
1. Find TransactionRecord by id
2. Verify record's accountNumber belongs to userId
3. Return TransactionDetailResponse
```

### UC-04: Handle Transfer Events (Kafka Consumer)
```
On TransferCompletedEvent:
1. Check idempotency (transactionId not already processed)
2. Create DEBIT record for source account
3. Create CREDIT record for destination account
4. Save both records

On TransferFailedEvent:
1. Check idempotency
2. Create FAILED record for audit
3. Save record
```

## Note on Account Ownership
Transaction History Service does not call Account Service to verify ownership.
Instead, it stores accountNumber with each record and the API Gateway forwards X-User-Id.
The controller verifies that the queried accountNumber has records belonging to the requesting user.
