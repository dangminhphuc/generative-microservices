# Caching TODO

## Tổng quan

Implement Spring Cache + Caffeine cho account-service theo thiết kế trong NFR Design Patterns (section 7).

**Trạng thái**: Chưa implement

---

## Task 1: Thêm dependencies

- [ ] Thêm `spring-boot-starter-cache` vào `account-service/pom.xml`
- [ ] Thêm `com.github.ben-manes.caffeine:caffeine` vào `account-service/pom.xml`

---

## Task 2: Tạo CacheConfig

- [ ] Tạo class `CacheConfig` trong `infrastructure/config/`
- [ ] Annotate `@Configuration` + `@EnableCaching`
- [ ] Khai báo `CaffeineCacheManager` bean với 3 caches:
  - `users-by-email` — TTL 5 min, max 10,000
  - `accounts-by-number` — TTL 10 min, max 10,000
  - `accounts-by-userId` — TTL 5 min, max 10,000
- [ ] Enable `recordStats` để Micrometer thu thập cache metrics

---

## Task 3: Thêm cache annotations vào service layer

- [ ] `AuthenticationService.login()` — `@CacheEvict(value = "users-by-email", key = "#command.email()")`
- [ ] `BankAccountService.getAccountsByUserId()` — `@Cacheable(value = "accounts-by-userId", key = "#userId")`
- [ ] `BankAccountService.createAccount()` — `@CacheEvict(value = "accounts-by-userId", key = "#command.userId()")`
- [ ] `BankAccountService.getByAccountNumber()` (hoặc tương đương) — `@Cacheable(value = "accounts-by-number", key = "#accountNumber")`
- [ ] `DebitCreditService.processTransfer()` — `@Caching(evict = {...})` evict cả `accounts-by-number` (source + dest) và `accounts-by-userId` (allEntries)

---

## Task 4: Verify cache metrics

- [ ] Confirm `/actuator/prometheus` expose `cache_gets_total`, `cache_evictions_total`, `cache_size`
- [ ] Verify metrics tagged by cache name và result (hit/miss)

---

## Task 5: Testing

- [ ] Unit test: verify cache hit (gọi 2 lần, repository chỉ gọi 1 lần)
- [ ] Unit test: verify cache eviction (write operation → next read gọi lại repository)
- [ ] Integration test: verify end-to-end caching behavior

---

## Lưu ý

- Caffeine là local cache — mỗi instance có cache riêng. Chấp nhận được cho dev/staging.
- Production multi-instance: cân nhắc migrate sang Redis (chỉ đổi config, không đổi code nhờ Spring Cache abstraction).
- Tham khảo: `aidlc-docs/construction/account-service/nfr-design/nfr-design-patterns.md` (section 7)
