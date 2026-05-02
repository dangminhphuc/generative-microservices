# Rate Limiting TODO

## Tổng quan

Implement Redis-based rate limiting cho `api-gateway` sử dụng Spring Cloud Gateway
`RequestRateLimiter` filter + Redis token bucket algorithm.

**Trạng thái**: Chưa implement  
**Ưu tiên**: High — bảo vệ chống brute-force (đặc biệt `/api/auth/**`) và DDoS

---

## Design

### Thuật toán: Token Bucket (Redis)

Spring Cloud Gateway dùng Lua script trên Redis để implement token bucket:
- `replenishRate` — số request được thêm vào bucket mỗi giây (sustained rate)
- `burstCapacity` — dung lượng tối đa của bucket (burst rate)
- `requestedTokens` — số token mỗi request tiêu thụ (mặc định 1)

Key Redis: `request_rate_limiter.{keyResolver}.tokens` và `.timestamp`

### Per-route Config

| Route | replenishRate | burstCapacity | Lý do |
|-------|--------------|---------------|-------|
| `account-service-auth` (`/api/auth/**`) | 5 req/s | 10 | Chặn brute-force login/register |
| `account-service` (`/api/accounts/**`) | 50 req/s | 100 | API thông thường |
| `transfer-service` (`/api/transfers/**`) | 20 req/s | 40 | Giao dịch tài chính — thận trọng hơn |
| `transaction-history-service` (`/api/transactions/**`) | 30 req/s | 60 | Read-heavy, thoáng hơn |

### Key Resolver: Per-User (JWT Subject)

Rate limit theo `X-User-Id` header (được inject bởi `JwtAuthenticationFilter`).
Public paths (`/api/auth/**`) fallback về IP-based key vì chưa có user context.

---

## Task 1: Thêm Redis vào docker-compose.yml

- [ ] Thêm service `redis` vào `docker-compose.yml`:
  ```yaml
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3
  ```
- [ ] Thêm `depends_on: redis: condition: service_healthy` vào service `api-gateway`

---

## Task 2: Thêm dependencies vào `api-gateway/pom.xml`

- [ ] Thêm `spring-boot-starter-data-redis-reactive`:
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
  </dependency>
  ```
  > Reactive Redis client là bắt buộc — Spring Cloud Gateway chạy trên WebFlux stack.

---

## Task 3: Tạo `RateLimitConfig.java`

Tạo `api-gateway/src/main/java/com/fintech/gateway/config/RateLimitConfig.java`:

```java
@Configuration
public class RateLimitConfig {

    /**
     * Key resolver: dùng X-User-Id header (injected bởi JwtAuthenticationFilter).
     * Fallback về remote IP nếu header không có (public paths như /api/auth/**).
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            // Fallback: IP-based key cho public paths
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String ip = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    /** Auth endpoints — strict: 5 req/s sustained, burst 10 */
    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    /** Standard API endpoints — 50 req/s sustained, burst 100 */
    @Bean
    public RedisRateLimiter apiRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }

    /** Transfer endpoints — financial ops: 20 req/s sustained, burst 40 */
    @Bean
    public RedisRateLimiter transferRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }
}
```

---

## Task 4: Cấu hình `RequestRateLimiter` filter trong `application.yml`

Thêm filter vào từng route trong `api-gateway/src/main/resources/application.yml`:

```yaml
# Auth route — strict rate limit
- id: account-service-auth
  uri: lb://account-service
  predicates:
    - Path=/api/auth/**
  filters:
    - StripPrefix=1
    - name: RequestRateLimiter
      args:
        redis-rate-limiter: "#{@authRateLimiter}"
        key-resolver: "#{@userKeyResolver}"

# Accounts route
- id: account-service
  uri: lb://account-service
  predicates:
    - Path=/api/accounts/**
  filters:
    - StripPrefix=1
    - name: RequestRateLimiter
      args:
        redis-rate-limiter: "#{@apiRateLimiter}"
        key-resolver: "#{@userKeyResolver}"

# Transfers route — financial ops
- id: transfer-service
  uri: lb://transfer-service
  predicates:
    - Path=/api/transfers/**
  filters:
    - StripPrefix=1
    - name: RequestRateLimiter
      args:
        redis-rate-limiter: "#{@transferRateLimiter}"
        key-resolver: "#{@userKeyResolver}"

# Transaction history route
- id: transaction-history-service
  uri: lb://transaction-history-service
  predicates:
    - Path=/api/transactions/**
  filters:
    - StripPrefix=1
    - name: RequestRateLimiter
      args:
        redis-rate-limiter: "#{@apiRateLimiter}"
        key-resolver: "#{@userKeyResolver}"
```

Thêm Redis connection config:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

---

## Task 5: Cấu hình Redis cho từng profile

**`application-docker.yml`** — thêm:
```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379
```

**`application-dev.yml`** — thêm (local Redis hoặc disable rate limiting):
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

> **Lưu ý dev**: Nếu không muốn chạy Redis local khi dev, có thể dùng
> `spring.cloud.gateway.filter.request-rate-limiter.deny-empty-key=false`
> để skip rate limiting khi Redis không available.

---

## Task 6: Cập nhật `pom.xml` description

- [ ] Sửa `<description>` trong `api-gateway/pom.xml`:
  ```xml
  <description>Spring Cloud Gateway — routing, JWT validation, CORS, rate limiting</description>
  ```
  > Sau khi implement xong mới cập nhật description để phản ánh đúng thực tế.

---

## Task 7: Testing

- [ ] Unit test `RateLimitConfig.userKeyResolver()`:
  - Request có `X-User-Id: 42` → key = `"user:42"`
  - Request không có header, IP = `127.0.0.1` → key = `"ip:127.0.0.1"`
- [ ] Integration test với embedded Redis (`com.github.codemonstur:embedded-redis` hoặc Testcontainers Redis):
  - Gửi 11 request liên tiếp đến `/api/auth/login` → request thứ 11 nhận HTTP 429
  - Gửi request đến `/api/accounts/123` với valid JWT → không bị rate limit ở mức thấp
- [ ] Verify header response khi bị rate limit:
  - `X-RateLimit-Remaining: 0`
  - `X-RateLimit-Replenish-Rate: 5`
  - HTTP status: `429 Too Many Requests`

---

## Lưu ý kỹ thuật

- **Redis phải là reactive client** (`spring-boot-starter-data-redis-reactive`) — blocking client không tương thích với WebFlux.
- **`@Primary` trên `userKeyResolver`** — Spring Cloud Gateway auto-wire `KeyResolver` bean, cần đánh dấu primary nếu có nhiều bean.
- **Rate limit headers** — Spring Cloud Gateway tự động thêm `X-RateLimit-*` headers vào response.
- **Distributed rate limiting** — Redis đảm bảo rate limit được chia sẻ giữa nhiều instance gateway (khác với in-memory).
- **Testcontainers** — khuyến nghị dùng `org.testcontainers:testcontainers` + `redis` image cho integration test thay vì embedded Redis (embedded Redis không support Redis 7+).
