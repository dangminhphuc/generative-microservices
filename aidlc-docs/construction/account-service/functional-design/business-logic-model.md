# Business Logic Model — Account Service

## Use Case Flows

### UC-01: Register User

```
Input: RegisterUserCommand(email, password, fullName, phoneNumber)

1. Validate input format (email, password strength, phone format)
2. Check email uniqueness → EmailAlreadyExistsException
3. Check phone uniqueness → PhoneAlreadyExistsException
4. Create User aggregate:
   - Hash password with BCrypt
   - Set status = ACTIVE
   - Set failedLoginAttempts = 0
5. Save User via UserRepository
6. Return RegisterUserResponse(userId, email, createdAt)
```

### UC-02: Login

```
Input: LoginCommand(email, password)

1. Find user by email → InvalidCredentialsException if not found
2. Check if account is locked (lockedUntil > now) → AccountLockedException
3. Verify password with BCrypt:
   - If FAIL:
     a. Increment failedLoginAttempts
     b. If failedLoginAttempts >= 5: set lockedUntil = now + 15min, status = LOCKED
     c. Save user
     d. Throw InvalidCredentialsException
   - If SUCCESS:
     a. Reset failedLoginAttempts = 0
     b. If status == LOCKED and lockedUntil <= now: set status = ACTIVE
     c. Save user
4. Generate JWT access token (15 min expiry)
5. Generate JWT refresh token (7 days expiry)
6. Evict cache: users-by-email (user state changed)
7. Return LoginResponse(accessToken, refreshToken, expiresIn)
```

### UC-03: Refresh Token

```
Input: RefreshTokenCommand(refreshToken)

1. Parse and validate refresh token (signature, expiry, type="refresh")
   → InvalidTokenException / TokenExpiredException
2. Extract userId from token
3. Find user → ResourceNotFoundException
4. Generate new access token + refresh token
5. Return LoginResponse(accessToken, refreshToken, expiresIn)
```

### UC-04: Create Bank Account

```
Input: CreateBankAccountCommand(userId)

1. Find user by userId → UserNotFoundException
2. Check user status == ACTIVE
3. Count existing accounts for user
   → MaxAccountsReachedException if count >= 5
4. Generate unique account number (10 digits)
5. Create BankAccount aggregate:
   - balance = Money.zero()
   - status = ACTIVE
6. Save BankAccount via BankAccountRepository
7. Evict cache: accounts-by-userId (new account added)
8. Publish AccountCreatedEvent
9. Return CreateBankAccountResponse(accountId, accountNumber, balance, status)
```

### UC-05: Get Balance

```
Input: GetBalanceQuery(userId, accountId?)

1. If accountId provided:
   a. Find account by id → AccountNotFoundException
   b. Verify account.userId == query.userId → AccessDeniedException
   c. Return single account balance
2. If accountId not provided:
   a. Find all accounts by userId (cached: accounts-by-userId)
   b. Return list (may be empty)
```

### UC-06: Get Account By Number (Internal API)

```
Input: GetAccountByNumberQuery(accountNumber)

1. Find account by accountNumber (cached: accounts-by-number) → AccountNotFoundException
2. Find user by account.userId (for owner name)
3. Return AccountInfoResponse(accountId, accountNumber, ownerName, status)
```

### UC-07: Debit Account (Internal — Kafka consumer)

```
Input: TransferInitiatedEvent (from Kafka)

1. Find account by sourceAccountNumber → publish DebitFailedEvent
2. Validate account status == ACTIVE → publish DebitFailedEvent
3. Validate balance >= amount → publish DebitFailedEvent("Insufficient balance")
4. Execute debit: account.debit(amount, transferId)
5. Save account (optimistic locking)
6. Find destination account, execute credit: account.credit(amount, transferId)
7. Save destination account
8. Evict caches: accounts-by-number (source + dest), accounts-by-userId (allEntries)
9. Events published by aggregate methods (AccountDebitedEvent, AccountCreditedEvent)
```

---

## Domain Services

### AccountNumberGenerator
- Generates unique 10-digit account numbers
- Strategy: Random generation + uniqueness check via repository
- Retry on collision (max 3 attempts)

### JwtTokenService (Infrastructure concern — defined here for completeness)
- `generateAccessToken(User user)` → JWT string (15 min)
- `generateRefreshToken(User user)` → JWT string (7 days)
- `validateToken(String token)` → Claims
- `extractUserId(String token)` → UUID
- Secret key: configurable via application properties
- Algorithm: HS256
