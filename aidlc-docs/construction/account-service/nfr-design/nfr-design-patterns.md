# NFR Design Patterns — Account Service

## 1. Security Patterns

### 1.1 Authentication Flow (SECURITY-08, SECURITY-12)

```
Client → API Gateway (JWT filter) → Account Service
                                         |
                                    [Public endpoints: /auth/**]
                                    [Protected endpoints: /accounts/**]
```

- **Stateless JWT**: No server-side session. Token contains userId + email.
- **Token Rotation**: Access token (15 min) + Refresh token (7 days). Refresh generates new pair.
- **Brute Force Protection**: Progressive lockout — 5 failed attempts → 15 min lock. Counter resets on success.

### 1.2 Authorization Pattern (SECURITY-08)

```java
// Object-level authorization — every endpoint verifies ownership
@PreAuthorize or manual check:
  userId from JWT header (X-User-Id) == resource.userId
  → 403 Forbidden if mismatch
```

- Deny-by-default: All endpoints require authentication except `/auth/**`
- Object-level: Every account query verifies `account.userId == requestUserId`

### 1.3 Input Validation Pattern (SECURITY-05)

```
Request DTO → Jakarta Bean Validation (@NotBlank, @Email, @Size, @Pattern)
           → Custom validators (PasswordStrength, PhoneNumber format)
           → Domain validation (business rules in aggregate)
```

Three-layer validation:
1. **DTO level**: Format validation (annotations)
2. **Application level**: Business rule pre-checks (uniqueness)
3. **Domain level**: Aggregate invariants (balance >= 0)

### 1.4 Error Handling Pattern (SECURITY-09, SECURITY-15)

```java
@RestControllerAdvice GlobalExceptionHandler
  → BaseException subtypes → structured error response
  → Unexpected exceptions → generic "Internal server error" (no stack trace)
  → Validation errors → field-level error details
```

Response format:
```json
{
  "errorCode": "INSUFFICIENT_BALANCE",
  "message": "Số dư không đủ",
  "timestamp": "2026-04-23T00:00:00Z",
  "fieldErrors": {}
}
```

### 1.5 Credential Storage Pattern (SECURITY-12)

- BCrypt with default strength (10 rounds)
- Password never stored in plain text, never logged
- No hardcoded secrets — JWT secret via environment variable

---

## 2. Resilience Patterns

### 2.1 Optimistic Locking (Concurrent Access)

```java
@Version
private long version;

// On concurrent debit:
// Thread A reads version=1, Thread B reads version=1
// Thread A saves version=2 → success
// Thread B saves version=2 → OptimisticLockException → retry or fail
```

Applied to: `BankAccount` entity (concurrent debit/credit from transfer saga)

### 2.2 Idempotent Event Processing

```
Kafka consumer receives TransferInitiatedEvent
  → Check if transferId already processed (idempotency key)
  → If yes: skip (already debited/credited)
  → If no: process debit/credit, record transferId
```

Prevents double-debit on Kafka redelivery (at-least-once semantics).

### 2.3 Graceful Shutdown

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
server:
  shutdown: graceful
```

Completes in-flight requests before stopping.

---

## 3. Performance Patterns

### 3.1 Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
```

### 3.2 Database Indexing Strategy

| Table | Index | Purpose |
|---|---|---|
| users | UNIQUE(email) | Login lookup, uniqueness |
| users | UNIQUE(phone_number) | Registration uniqueness |
| bank_accounts | UNIQUE(account_number) | Transfer lookup |
| bank_accounts | INDEX(user_id) | Balance query by user |

### 3.3 Pagination

- Balance query returns all accounts for a user (max 5 — no pagination needed)
- Internal APIs return single results

---

## 4. Observability Patterns

### 4.1 Structured Logging (SECURITY-03)

```
Format: JSON
Fields: timestamp, level, logger, message, correlationId, userId, traceId
Excluded: passwords, tokens, PII in log output
```

### 4.2 Metrics Collection

```java
// Custom Micrometer counters
Counter loginSuccess = Counter.builder("auth.login.success").register(registry);
Counter loginFailure = Counter.builder("auth.login.failure").register(registry);
Counter accountCreated = Counter.builder("account.created").register(registry);
Timer debitTimer = Timer.builder("account.debit.duration").register(registry);
```

### 4.3 Correlation ID Propagation

```
API Gateway sets X-Correlation-Id header (UUID)
  → Account Service reads header → stores in MDC
  → All log entries include correlationId
  → Kafka messages include correlationId in headers
```

---

## 5. Data Integrity Patterns

### 5.1 Transaction Boundaries

| Operation | Transaction Scope |
|---|---|
| Register User | Single transaction (save user) |
| Create Account | Single transaction (save account + publish event) |
| Debit + Credit | Single transaction (debit source + credit dest + publish events) |

### 5.2 Outbox Pattern (Future Enhancement)

Currently: Domain events published directly via Kafka after transaction commit.
Risk: If Kafka publish fails after DB commit, event is lost.
Future: Transactional Outbox pattern — save event to outbox table in same transaction, relay to Kafka asynchronously.

Note: Acceptable for development phase. Outbox pattern recommended for production.


---

## 7. Caching Patterns

### 7.1 Cache Architecture

```
Client Request
    |
    v
Controller → Service → @Cacheable → Cache Hit? → Return cached
                                         |
                                         No
                                         |
                                    Repository → DB → Store in cache → Return
```

- **Cache-aside (read-through)**: `@Cacheable` on read methods — check cache first, fallback to DB, populate cache on miss.
- **Write-invalidate**: `@CacheEvict` on write methods — evict stale entries on mutation, next read repopulates.
- **Local only**: Caffeine in-process cache — no network hop, sub-millisecond reads. Acceptable for single-instance or stateless services behind load balancer (each instance warms its own cache).

### 7.2 Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheSpecification("maximumSize=10000,expireAfterWrite=5m,recordStats");
        // Per-cache overrides via registerCustomCache() if needed
        return manager;
    }
}
```

**Cache definitions**:

| Cache Name | TTL | Max Size | Key | Value |
|---|---|---|---|---|
| `users-by-email` | 5 min | 10,000 | email (String) | User entity |
| `accounts-by-number` | 10 min | 10,000 | accountNumber (String) | BankAccount entity |
| `accounts-by-userId` | 5 min | 10,000 | userId (UUID) | List\<BankAccount\> |

### 7.3 Caching Annotations Placement

```java
// AuthenticationService — Login
@CacheEvict(value = "users-by-email", key = "#command.email()")
public LoginResponse login(LoginCommand command) { ... }

// RegisterUserService
// No caching — new user, not yet queried

// BankAccountService — Get Balance (by userId)
@Cacheable(value = "accounts-by-userId", key = "#userId")
public List<BankAccountResponse> getAccountsByUserId(UUID userId) { ... }

// BankAccountService — Create Account
@CacheEvict(value = "accounts-by-userId", key = "#command.userId()")
public CreateBankAccountResponse createAccount(CreateBankAccountCommand command) { ... }

// InternalAccountController / DebitCreditService — Get by Number
@Cacheable(value = "accounts-by-number", key = "#accountNumber")
public BankAccount getByAccountNumber(String accountNumber) { ... }

// DebitCreditService — Debit/Credit
@Caching(evict = {
    @CacheEvict(value = "accounts-by-number", key = "#sourceAccountNumber"),
    @CacheEvict(value = "accounts-by-number", key = "#destAccountNumber"),
    @CacheEvict(value = "accounts-by-userId", allEntries = true)
})
public void processTransfer(...) { ... }
```

### 7.4 Cache Metrics (Observability)

Caffeine with `recordStats` enabled exposes via Micrometer:
- `cache.gets` (tag: result=hit|miss) — hit rate monitoring
- `cache.evictions` — eviction pressure
- `cache.size` — current entry count

Exposed at `/actuator/prometheus` alongside existing custom metrics.

### 7.5 Cache Consistency Considerations

| Scenario | Behavior | Acceptable? |
|---|---|---|
| Login updates failedAttempts → another request reads stale User | Stale read within 5 min TTL | ✅ Yes — worst case: one extra login attempt before lock visible |
| Debit changes balance → balance query returns stale | Evicted on debit — next read fetches fresh | ✅ Yes — explicit eviction |
| Two instances cache same user | Each instance has independent cache | ✅ Yes — acceptable for dev/staging. For production: consider Redis |
| Account created → list query returns stale | Evicted on create — next read fetches fresh | ✅ Yes — explicit eviction |

**Production consideration**: For multi-instance production deployment, migrate from Caffeine (local) to Redis (distributed) to ensure cross-instance cache consistency. The Spring Cache abstraction makes this a configuration-only change.

---

## 6. Logging Implementation Detail (Supplement — 2026-04-26)

### 6.1 Structured JSON Logging

**Dependency**: `net.logstash.logback:logstash-logback-encoder:8.0`

**Configuration**: `logback-spring.xml` with profile-based appenders:
- **dev profile**: Console appender (human-readable pattern with correlationId)
- **prod/default profile**: JSON appender via LogstashEncoder (machine-parseable)

**JSON fields**: timestamp, level, logger_name, message, correlationId, thread_name, stack_trace (errors only)

### 6.2 RequestLoggingFilter

**Component**: `RequestLoggingFilter extends OncePerRequestFilter`
**Order**: After CorrelationIdFilter (Ordered.HIGHEST_PRECEDENCE + 1)
**Logs**: HTTP method, URI, status code, response time (ms)
**Excludes**: Request/response body, Authorization header, PII
**Security**: SECURITY-03 compliant — no sensitive data in logs

### 6.3 Service-Level Logging Strategy

| Service | Log Events | Level |
|---|---|---|
| AuthenticationService | Login success, login failure (no credentials), account locked | INFO / WARN |
| RegisterUserService | Registration success, duplicate email/phone rejection | INFO / WARN |
| BankAccountService | Account created, balance queried | INFO |
| DebitCreditService | Transfer processed, transfer failed (existing) | INFO / ERROR |

**Rule**: Never log passwords, tokens, full email, full phone number. Use masked format for PII if needed.

### 6.4 Custom Micrometer Counters

Implemented in service classes via constructor-injected `MeterRegistry`:
- `auth.login.success` (tag: none)
- `auth.login.failure` (tag: reason)
- `auth.register.success` (tag: none)
- `account.created` (tag: none)
- `account.debit.success` / `account.debit.failure` (tag: none)

### 6.5 Log Level Configuration

```yaml
logging:
  level:
    com.fintech: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
    org.apache.kafka: WARN
```
