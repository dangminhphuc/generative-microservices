# Domain Entities — Account Service

## Aggregate 1: User

### User (Aggregate Root)

| Attribute | Type | Constraints |
|---|---|---|
| id | UUID | PK, auto-generated |
| email | Email (VO) | Unique, not null, valid email format |
| password | HashedPassword (VO) | Not null, min 8 chars (raw), hashed with BCrypt |
| fullName | String | Not null, max 100 chars |
| phoneNumber | PhoneNumber (VO) | Unique, not null, valid VN phone format |
| status | UserStatus (enum) | ACTIVE, LOCKED, INACTIVE |
| failedLoginAttempts | int | Default 0, max 5 before lock |
| lockedUntil | Instant | Null if not locked |
| createdAt | Instant | Auto-set |
| updatedAt | Instant | Auto-updated |

### Value Objects

**Email**
- Immutable, validated format (regex)
- Normalized to lowercase

**PhoneNumber**
- Immutable, validated VN phone format (10 digits, starts with 0)
- Stored without formatting

**HashedPassword**
- Wraps BCrypt-encoded string
- Factory method: `fromRaw(String raw)` → validates strength → hashes
- Method: `matches(String raw)` → verifies

**UserStatus** (Enum)
- `ACTIVE` — normal operation
- `LOCKED` — temporarily locked (brute force protection)
- `INACTIVE` — deactivated

---

## Aggregate 2: BankAccount

### BankAccount (Aggregate Root)

| Attribute | Type | Constraints |
|---|---|---|
| id | UUID | PK, auto-generated |
| accountNumber | AccountNumber (VO) | Unique, 10 digits, auto-generated |
| userId | UUID | FK to User, not null |
| balance | Money (VO) | Default 0 VND, never negative |
| status | AccountStatus (enum) | ACTIVE, FROZEN, CLOSED |
| createdAt | Instant | Auto-set |
| updatedAt | Instant | Auto-updated |
| version | long | Optimistic locking |

### Methods

```
BankAccount.debit(Money amount, String transferId) → void
  - Pre: status == ACTIVE, balance >= amount
  - Post: balance = balance - amount
  - Raises: InsufficientBalanceException, AccountFrozenException
  - Publishes: AccountDebitedEvent

BankAccount.credit(Money amount, String transferId) → void
  - Pre: status == ACTIVE
  - Post: balance = balance + amount
  - Publishes: AccountCreditedEvent
```

### Value Objects

**AccountNumber**
- Immutable, 10-digit string
- Generated via `AccountNumberGenerator` (domain service)
- Format: numeric only, zero-padded

**AccountStatus** (Enum)
- `ACTIVE` — normal operation, can debit/credit
- `FROZEN` — cannot debit or credit
- `CLOSED` — permanently closed

---

## Domain Events (published by Account Service)

| Event | Trigger | Payload |
|---|---|---|
| AccountCreatedEvent | BankAccount created | accountId, accountNumber, userId |
| AccountDebitedEvent | Successful debit | accountNumber, amount, currency, transferId |
| AccountCreditedEvent | Successful credit | accountNumber, amount, currency, transferId |
| DebitFailedEvent | Debit failed | accountNumber, amount, currency, transferId, reason |

---

## Testable Properties (PBT-01)

| Component | Property Category | Description |
|---|---|---|
| Money | Round-trip | serialize(deserialize(money)) == money |
| Money | Invariant | a.add(b).subtract(b) == a |
| Money | Commutativity | a.add(b) == b.add(a) |
| Email | Round-trip | Email.of(email.toString()) == email |
| BankAccount.debit/credit | Invariant | debit(x) then credit(x) restores original balance |
| AccountNumber | Invariant | Generated numbers are always 10 digits |
| HashedPassword | Round-trip (partial) | fromRaw(raw).matches(raw) == true |
