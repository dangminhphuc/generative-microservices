# Integration Test Instructions — Fintech Platform

## Purpose
Test interactions between services via REST and Kafka to ensure end-to-end flows work correctly.

## Prerequisites
- Docker Compose infrastructure running (PostgreSQL x3, Kafka, Eureka)
- All services running

## Setup
```bash
# Start all infrastructure
docker compose up -d

# Wait for services to register with Eureka (~60 seconds)
# Verify at http://localhost:8761
```

## Integration Test Scenarios

### Scenario 1: User Registration → Login → Create Account
```bash
# 1. Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@1234",
    "fullName": "Nguyen Van Test",
    "phoneNumber": "0901234567"
  }'
# Expected: 201 Created, returns userId

# 2. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@1234"
  }'
# Expected: 200 OK, returns accessToken + refreshToken

# 3. Create bank account (use accessToken from step 2)
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <accessToken>"
# Expected: 201 Created, returns accountNumber

# 4. Get balance
curl http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <accessToken>"
# Expected: 200 OK, returns account list with balance = 0
```

### Scenario 2: Fund Transfer (End-to-End Saga)
```bash
# Prerequisites: 2 users with accounts, source account has balance > 0
# (Seed data or manual deposit needed)

# 1. Preview transfer
curl -X POST http://localhost:8080/api/transfers/preview \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "0987654321",
    "amount": 100000,
    "description": "Test transfer"
  }'
# Expected: 200 OK, returns preview with masked dest name

# 2. Initiate transfer
curl -X POST http://localhost:8080/api/transfers \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "0987654321",
    "amount": 100000,
    "description": "Test transfer"
  }'
# Expected: 201 Created, status=PENDING

# 3. Wait for saga completion (~2-5 seconds for Kafka processing)

# 4. Check transaction history
curl http://localhost:8080/api/transactions/account/1234567890 \
  -H "Authorization: Bearer <accessToken>"
# Expected: DEBIT record for source account

curl http://localhost:8080/api/transactions/account/0987654321 \
  -H "Authorization: Bearer <accessToken2>"
# Expected: CREDIT record for destination account
```

### Scenario 3: Transfer Validation Failures
```bash
# Same account transfer
# Expected: 422, SAME_ACCOUNT

# Amount below minimum (< 1,000 VND)
# Expected: 422, MINIMUM_AMOUNT

# Insufficient balance
# Expected: Transfer created as PENDING, then FAILED via saga
```

### Scenario 4: Transaction History Filtering
```bash
# Filter by date range
curl "http://localhost:8080/api/transactions/account/1234567890/filter?startDate=2026-04-01&endDate=2026-04-30" \
  -H "Authorization: Bearer <accessToken>"

# Filter by type
curl "http://localhost:8080/api/transactions/account/1234567890/filter?type=DEBIT" \
  -H "Authorization: Bearer <accessToken>"
```

## Cleanup
```bash
docker compose down -v
```
