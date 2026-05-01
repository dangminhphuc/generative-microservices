# Unit Test Execution — Fintech Platform

## Run All Unit Tests
```bash
mvn test
```

## Run Tests Per Module
```bash
# Common module
mvn test -pl common

# Account Service
mvn test -pl account-service

# Transfer Service
mvn test -pl transfer-service

# Transaction History Service
mvn test -pl transaction-history-service
```

## Test Categories

### Domain Layer Tests (Pure Unit Tests)
- Value Object tests: Email, PhoneNumber, AccountNumber, Money
- Aggregate tests: User (login attempts, locking), BankAccount (debit/credit)
- Domain Service tests: AccountNumberGenerator, TransferValidationService
- No Spring context, no mocks — pure Java

### Application Layer Tests (Use Case Tests)
- Use case tests with mocked outbound ports
- Business rule validation tests
- Error scenario tests
- Uses Mockito for port mocking

### Property-Based Tests (jqwik)
- Money arithmetic properties (round-trip, commutativity, invariants)
- Email/PhoneNumber parsing round-trip
- TransferValidationService invariants
- Separate test classes with `@Property` annotations

## Expected Results

| Module | Unit Tests | PBT Tests | Total |
|---|---|---|---|
| common | ~10 | ~5 | ~15 |
| account-service | ~25 | ~7 | ~32 |
| transfer-service | ~15 | ~4 | ~19 |
| transaction-history-service | ~10 | ~3 | ~13 |
| **Total** | **~60** | **~19** | **~79** |

## Test Reports
```bash
# Generate test reports
mvn surefire-report:report

# Reports location
target/surefire-reports/
```

## PBT-Specific Instructions (jqwik)

### Seed Logging
jqwik automatically logs seeds on failure. To reproduce a failing test:
```bash
# jqwik stores seeds in .jqwik-database file
# Re-run with same seed for reproducibility
mvn test -pl account-service -Dtest=MoneyPropertyTest
```

### Shrinking
jqwik shrinking is enabled by default. On failure, the output shows:
- Original failing input
- Shrunk minimal failing input
- Seed for reproduction
