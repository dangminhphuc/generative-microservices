# Domain Entities — Transaction History Service

## Aggregate: TransactionRecord

### TransactionRecord (Aggregate Root)

| Attribute | Type | Constraints |
|---|---|---|
| id | UUID | PK, auto-generated |
| transactionId | String | The transfer ID that triggered this record |
| accountNumber | String | The account this record belongs to |
| type | TransactionType (enum) | CREDIT or DEBIT |
| amount | Money (VO) | Positive |
| balanceBefore | Money (VO) | Balance before transaction |
| balanceAfter | Money (VO) | Balance after transaction |
| counterpartyAccountNumber | String | The other account in the transfer |
| description | String | Transfer description |
| status | String | COMPLETED or FAILED |
| createdAt | Instant | When the record was created |

### TransactionType (Enum)
- `CREDIT` — money received
- `DEBIT` — money sent

### No Domain Events
Transaction History Service is a pure consumer — it does not publish events.

## Testable Properties (PBT-01)

| Component | Property Category | Description |
|---|---|---|
| TransactionRecord creation | Invariant | Every TransferCompletedEvent creates exactly 2 records (1 DEBIT + 1 CREDIT) |
| Pagination | Invariant | Page size never exceeds requested size |
| Date filter | Invariant | All returned records fall within requested date range |
