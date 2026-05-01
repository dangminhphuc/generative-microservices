# Domain Entities — Transfer Service

## Aggregate: Transfer

### Transfer (Aggregate Root)

| Attribute | Type | Constraints |
|---|---|---|
| id | UUID | PK, auto-generated |
| sourceAccountNumber | AccountNumber (VO) | Not null |
| destinationAccountNumber | AccountNumber (VO) | Not null, != source |
| amount | Money (VO) | Not null, positive, >= 1,000 VND |
| description | String | Max 200 chars, nullable |
| status | TransferStatus (enum) | PENDING, COMPLETED, FAILED |
| userId | UUID | Not null, owner of source account |
| createdAt | Instant | Auto-set |
| completedAt | Instant | Set on completion/failure |

### Value Objects

**TransferStatus** (Enum)
- `PENDING` — initiated, awaiting debit/credit
- `COMPLETED` — debit + credit successful
- `FAILED` — debit failed or timeout

**AccountReference** (reuses AccountNumber from common)

### Domain Events

| Event | Trigger | Payload |
|---|---|---|
| TransferInitiatedEvent | Transfer created (PENDING) | transferId, source, dest, amount, currency, description |
| TransferCompletedEvent | AccountDebitedEvent + AccountCreditedEvent received | transferId, source, dest, amount, currency, description, completedAt |
| TransferFailedEvent | DebitFailedEvent received or timeout | transferId, source, dest, amount, currency, reason, failedAt |

### Domain Service: TransferValidationService

Stateless validation:
- `validateMinimumAmount(Money amount)` — amount >= 1,000 VND
- `validateNotSameAccount(AccountNumber source, AccountNumber dest)` — source != dest
- `validateDailyLimit(AccountNumber source, Money todayTotal, Money newAmount)` — todayTotal + newAmount <= 500,000,000 VND
- `validateAccountActive(String accountStatus)` — status == "ACTIVE"

---

## Testable Properties (PBT-01)

| Component | Property Category | Description |
|---|---|---|
| Transfer status transitions | Invariant | PENDING can only go to COMPLETED or FAILED, never back |
| TransferValidationService | Invariant | Any amount < 1,000 VND always rejected |
| TransferValidationService | Invariant | Same source and dest always rejected |
| Money (from common) | Round-trip | serialize/deserialize round-trip |
