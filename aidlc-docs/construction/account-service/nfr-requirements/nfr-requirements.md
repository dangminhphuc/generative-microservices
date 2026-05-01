# NFR Requirements — Account Service

## 1. Performance

| Metric | Target | Rationale |
|---|---|---|
| API response time (p95) | < 200ms | Retail banking UX expectation |
| API response time (p99) | < 500ms | Acceptable tail latency |
| Login throughput | 100 req/s | Estimated peak concurrent logins |
| Registration throughput | 50 req/s | Lower frequency than login |
| Balance query throughput | 200 req/s | Most frequent operation |
| Database connection pool | 10-20 connections | HikariCP default tuning |

## 2. Scalability

| Aspect | Requirement |
|---|---|
| Horizontal scaling | Stateless service — multiple instances behind Eureka |
| Database | Single PostgreSQL instance (development phase), connection pooling |
| Session management | Stateless (JWT) — no server-side session storage |

## 3. Availability

| Aspect | Requirement |
|---|---|
| Target uptime | 99.9% (development/staging target) |
| Health check | Spring Boot Actuator `/actuator/health` |
| Graceful shutdown | Spring Boot graceful shutdown enabled |
| Circuit breaker | Not needed (Account Service has no outbound REST dependencies) |

## 4. Security (Security Baseline Extension)

| SECURITY Rule | Applicability | Implementation |
|---|---|---|
| SECURITY-01 | ✅ Applicable | PostgreSQL TLS connection, encryption at rest via Docker volume |
| SECURITY-03 | ✅ Applicable | SLF4J/Logback structured logging, correlation ID, no PII in logs |
| SECURITY-05 | ✅ Applicable | Jakarta Bean Validation on all DTOs, parameterized JPA queries |
| SECURITY-08 | ✅ Applicable | JWT validation, object-level authorization (userId check), RBAC |
| SECURITY-09 | ✅ Applicable | No default credentials, generic error responses, no stack traces |
| SECURITY-10 | ✅ Applicable | Maven dependency lock (pom.xml versions pinned), no `latest` Docker tags |
| SECURITY-11 | ✅ Applicable | Auth logic isolated in security module, rate limiting at gateway |
| SECURITY-12 | ✅ Applicable | BCrypt password hashing, brute force protection (5 attempts/15 min lock), JWT validation |
| SECURITY-15 | ✅ Applicable | Global exception handler, fail-closed on auth errors, resource cleanup |
| SECURITY-02 | N/A | Access logging handled at API Gateway level |
| SECURITY-04 | N/A | No HTML-serving endpoints (REST API only) |
| SECURITY-06 | N/A | No IAM policies (Docker Compose deployment) |
| SECURITY-07 | N/A | No cloud network config (Docker Compose) |
| SECURITY-13 | N/A | No CDN, no deserialization of untrusted external data |
| SECURITY-14 | Partial | Alerting deferred to Prometheus/Grafana setup, log retention via Docker volumes |

## 5. Testing (PBT Extension)

| PBT Rule | Applicability | Implementation |
|---|---|---|
| PBT-01 | ✅ | Properties identified in Functional Design (7 properties) |
| PBT-02 | ✅ | Round-trip tests: Money serialization, Email parsing, Password hashing |
| PBT-03 | ✅ | Invariant tests: Money arithmetic, AccountNumber format, debit/credit balance |
| PBT-04 | N/A | No idempotent operations in Account Service |
| PBT-05 | N/A | No oracle/reference implementations |
| PBT-06 | N/A | No complex stateful components (BankAccount state is simple) |
| PBT-07 | ✅ | Custom generators for Money, Email, PhoneNumber, AccountNumber |
| PBT-08 | ✅ | jqwik shrinking enabled, seed logging in CI |
| PBT-09 | ✅ | jqwik selected as PBT framework |
| PBT-10 | ✅ | PBT complements example-based tests, not replaces |

## 6. Observability

| Aspect | Implementation |
|---|---|
| Logging | SLF4J + Logback, structured JSON format |
| Metrics | Micrometer + Prometheus endpoint (`/actuator/prometheus`) |
| Health | Spring Boot Actuator (`/actuator/health`) |
| Custom metrics | `login.success.count`, `login.failure.count`, `account.created.count`, `transfer.debit.count` |
| Correlation ID | MDC-based, propagated via HTTP header `X-Correlation-Id` |

## 7. Caching

| Aspect | Requirement |
|---|---|
| Cache provider | Spring Cache abstraction + Caffeine (local, in-process) |
| Cached data | User lookup by email (login), BankAccount lookup by accountNumber (internal API), BankAccount list by userId (balance query) |
| TTL — User by email | 5 minutes (short — user state changes on login: failedAttempts, lockedUntil) |
| TTL — Account by number | 10 minutes (moderate — account data changes only on debit/credit) |
| TTL — Accounts by userId | 5 minutes (moderate — invalidated on account creation) |
| Max cache size | 10,000 entries per cache (bounded to prevent OOM) |
| Eviction policy | Size-based (LRU) + time-based (TTL expireAfterWrite) |
| Cache invalidation | Explicit eviction on write operations (register, debit, credit, account creation, login state change) |
| Consistency model | Eventually consistent — stale reads acceptable within TTL window |
| Metrics | Cache hit/miss counters via Micrometer (`cache.gets` tagged by result=hit/miss) |

### Cache Invalidation Rules

| Operation | Caches Evicted | Reason |
|---|---|---|
| Register User | — | New user, not yet cached |
| Login (success/failure) | `users-by-email` (by email) | failedLoginAttempts / lockedUntil changes |
| Refresh Token | — | No user state change |
| Create Bank Account | `accounts-by-userId` (by userId) | New account added to user's list |
| Debit Account | `accounts-by-number` (by accountNumber), `accounts-by-userId` (by userId) | Balance changed |
| Credit Account | `accounts-by-number` (by accountNumber), `accounts-by-userId` (by userId) | Balance changed |

---

## 8. Data Management

| Aspect | Implementation |
|---|---|
| Database | PostgreSQL 16, dedicated `account-db` |
| Schema migration | Flyway |
| Connection pool | HikariCP (Spring Boot default) |
| Transaction isolation | READ_COMMITTED (default) |
| Optimistic locking | `@Version` on BankAccount entity |
