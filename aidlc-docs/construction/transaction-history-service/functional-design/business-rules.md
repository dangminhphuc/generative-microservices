# Business Rules — Transaction History Service

## BR-TXH-01: View Transaction History

| Rule ID | Rule |
|---|---|
| BR-TXH-01.1 | User can only view transactions for their own accounts |
| BR-TXH-01.2 | Results paginated (default 20 items/page) |
| BR-TXH-01.3 | Sorted by createdAt DESC (newest first) |
| BR-TXH-01.4 | Empty list if no transactions (not an error) |

## BR-TXH-02: Filter Transactions

| Rule ID | Rule |
|---|---|
| BR-TXH-02.1 | Filter by date range (startDate, endDate) |
| BR-TXH-02.2 | Filter by transaction type (CREDIT/DEBIT) |
| BR-TXH-02.3 | startDate must be before endDate |
| BR-TXH-02.4 | Max date range: 12 months |
| BR-TXH-02.5 | Multiple filters combined with AND logic |

## BR-TXH-03: View Transaction Detail

| Rule ID | Rule |
|---|---|
| BR-TXH-03.1 | User can only view detail of transactions for their own accounts |
| BR-TXH-03.2 | Return full detail including balanceBefore, balanceAfter |

## BR-TXH-04: Event Consumption (Kafka)

| Rule ID | Rule |
|---|---|
| BR-TXH-04.1 | On TransferCompletedEvent: create 2 records (DEBIT for source, CREDIT for dest) |
| BR-TXH-04.2 | On TransferFailedEvent: create 1 FAILED record for audit |
| BR-TXH-04.3 | Idempotent: skip if transactionId already exists |

## Error Handling

| Error | HTTP Status | Error Code |
|---|---|---|
| Access denied | 403 | ACCESS_DENIED |
| Transaction not found | 404 | TRANSACTION_NOT_FOUND |
| Invalid date range | 400 | INVALID_DATE_RANGE |
| Date range too large | 400 | DATE_RANGE_TOO_LARGE |
