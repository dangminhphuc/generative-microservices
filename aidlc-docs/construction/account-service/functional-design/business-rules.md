# Business Rules — Account Service

## BR-ACC-01: User Registration

| Rule ID | Rule | Validation |
|---|---|---|
| BR-ACC-01.1 | Email must be unique | Check UserRepository.existsByEmail() |
| BR-ACC-01.2 | Phone number must be unique | Check UserRepository.existsByPhoneNumber() |
| BR-ACC-01.3 | Email must be valid format | Regex validation |
| BR-ACC-01.4 | Password minimum 8 chars, must contain uppercase, lowercase, digit, special char | Validation in HashedPassword.fromRaw() |
| BR-ACC-01.5 | Full name max 100 chars, not blank | String validation |
| BR-ACC-01.6 | Concurrent registration with same email: only one succeeds | Database unique constraint + optimistic handling |

---

## BR-ACC-02: Authentication (Login)

| Rule ID | Rule | Validation |
|---|---|---|
| BR-ACC-02.1 | Credentials must match (email + password) | BCrypt.matches() |
| BR-ACC-02.2 | Error message must not reveal which field is wrong | Generic "Email hoặc mật khẩu không đúng" |
| BR-ACC-02.3 | After 5 consecutive failed attempts → lock account 15 min | failedLoginAttempts >= 5 → set lockedUntil = now + 15min |
| BR-ACC-02.4 | Locked account cannot login | Check lockedUntil > now → reject |
| BR-ACC-02.5 | Successful login resets failed attempts counter | Set failedLoginAttempts = 0 |
| BR-ACC-02.6 | Access token expires in 15 minutes | JWT exp claim |
| BR-ACC-02.7 | Refresh token expires in 7 days | JWT exp claim |

### Token Structure
```
Access Token Claims:
  sub: userId (UUID)
  email: user email
  iat: issued at
  exp: expiration (15 min)

Refresh Token Claims:
  sub: userId (UUID)
  type: "refresh"
  iat: issued at
  exp: expiration (7 days)
```

---

## BR-ACC-03: Bank Account Creation

| Rule ID | Rule | Validation |
|---|---|---|
| BR-ACC-03.1 | User must exist and be ACTIVE | Check UserRepository |
| BR-ACC-03.2 | Account number must be unique (10 digits) | AccountNumberGenerator + DB unique constraint |
| BR-ACC-03.3 | Initial balance = 0 VND | Hardcoded in factory method |
| BR-ACC-03.4 | Initial status = ACTIVE | Hardcoded in factory method |
| BR-ACC-03.5 | Max 5 accounts per user | Check BankAccountRepository.countByUserId() |
| BR-ACC-03.6 | Publish AccountCreatedEvent on success | Domain event |

---

## BR-ACC-04: Balance Query

| Rule ID | Rule | Validation |
|---|---|---|
| BR-ACC-04.1 | User can only view own accounts | userId from JWT must match account.userId |
| BR-ACC-04.2 | Return all accounts if no accountId specified | Query by userId |
| BR-ACC-04.3 | Return empty list if no accounts exist | Not an error |

---

## BR-ACC-05: Internal Account Operations (called by Transfer Service)

| Rule ID | Rule | Validation |
|---|---|---|
| BR-ACC-05.1 | Debit: account must be ACTIVE | Check status |
| BR-ACC-05.2 | Debit: balance must be >= amount | Check balance >= amount |
| BR-ACC-05.3 | Debit: use optimistic locking (version field) | @Version on entity |
| BR-ACC-05.4 | Credit: account must be ACTIVE | Check status |
| BR-ACC-05.5 | On debit failure: publish DebitFailedEvent | Domain event with reason |
| BR-ACC-05.6 | On debit success: publish AccountDebitedEvent | Domain event |
| BR-ACC-05.7 | On credit success: publish AccountCreditedEvent | Domain event |

---

## Error Handling Summary

| Error | HTTP Status | Error Code | Message |
|---|---|---|---|
| Email already exists | 409 Conflict | EMAIL_ALREADY_EXISTS | Email đã được sử dụng |
| Phone already exists | 409 Conflict | PHONE_ALREADY_EXISTS | Số điện thoại đã được sử dụng |
| Invalid credentials | 401 Unauthorized | INVALID_CREDENTIALS | Email hoặc mật khẩu không đúng |
| Account locked | 423 Locked | ACCOUNT_LOCKED | Tài khoản tạm khóa |
| Token expired | 401 Unauthorized | TOKEN_EXPIRED | Token đã hết hạn |
| Max accounts reached | 422 Unprocessable | MAX_ACCOUNTS_REACHED | Đã đạt giới hạn số tài khoản |
| Account not found | 404 Not Found | ACCOUNT_NOT_FOUND | Tài khoản không tồn tại |
| Access denied | 403 Forbidden | ACCESS_DENIED | Không có quyền truy cập |
| Insufficient balance | 422 Unprocessable | INSUFFICIENT_BALANCE | Số dư không đủ |
| Account frozen | 422 Unprocessable | ACCOUNT_FROZEN | Tài khoản đã bị đóng băng |
