# API Gateway Hardening — Bugfix Design

## Overview

Module `api-gateway` tồn đọng 6 issues ảnh hưởng đến khả năng kiểm thử, bảo mật và vận hành.
Design này mô tả cách fix từng issue theo bug condition methodology, đảm bảo:

- **Fix Checking**: Mọi input thỏa `isBugCondition` đều được xử lý đúng sau khi fix.
- **Preservation Checking**: Mọi input không thỏa `isBugCondition` vẫn hoạt động y hệt trước khi fix.

Các thay đổi được giới hạn tối thiểu: không thêm dependency mới, không thay đổi routing logic hiện tại, không ảnh hưởng đến các service khác trong platform.

---

## Glossary

- **Bug_Condition (C)**: Điều kiện xác định input kích hoạt bug — khi `isBugCondition(X)` trả về `true`.
- **Property (P)**: Hành vi đúng kỳ vọng khi bug condition thỏa — kết quả sau khi fix phải thỏa `expectedBehavior(result)`.
- **Preservation**: Hành vi hiện tại đúng phải được giữ nguyên — với mọi `X` mà `NOT isBugCondition(X)`, `F(X) = F'(X)`.
- **F**: Hàm/cấu hình gốc (chưa fix).
- **F'**: Hàm/cấu hình sau khi fix.
- **JwtAuthenticationFilter**: `GlobalFilter` trong `filter/JwtAuthenticationFilter.java`, thực hiện xác thực JWT cho mọi request đến gateway.
- **SecurityConfig**: `@Configuration` trong `config/SecurityConfig.java`, cấu hình CORS via `CorsWebFilter`.
- **discovery locator**: Tính năng `spring.cloud.gateway.discovery.locator.enabled: true` tự động tạo route cho mọi service đăng ký với Eureka.
- **PUBLIC_PATHS**: Danh sách path không yêu cầu JWT: `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/actuator`.
- **WebTestClient**: HTTP client reactive dùng trong test Spring WebFlux, thay thế `TestRestTemplate` (không tương thích với reactive stack).

---

## Bug Details

### Issue 1 — Thiếu Integration Test

**Bug Condition**: Module `api-gateway` không có bất kỳ integration test nào.

```
FUNCTION isBugCondition_NoTest(X)
  INPUT: X of type GatewayModule
  OUTPUT: boolean

  RETURN testClassExists("ApiGatewayApplicationTests") = FALSE
END FUNCTION
```

**Examples**:
- Build thành công nhưng context bị misconfigure (ví dụ thiếu `jwt.secret`) → không có test nào phát hiện.
- `JwtAuthenticationFilter` bị xóa nhầm → không có test nào báo lỗi 401.
- `/actuator/health` bị tắt do config sai → không có test nào phát hiện.

---

### Issue 2 — Internal Endpoint Exposure

**Bug Condition**: Request từ bên ngoài đến path `/*/internal/**` hoặc `/internal/**` được forward thẳng đến downstream service do discovery locator không có rule chặn.

```
FUNCTION isBugCondition_InternalExposed(X)
  INPUT: X of type HttpRequest
  OUTPUT: boolean

  RETURN X.path MATCHES "^/([^/]+/)?internal(/.*)?$"
END FUNCTION
```

**Examples**:
- `GET /account-service/internal/accounts/123` → discovery locator forward đến `InternalAccountController` → trả về dữ liệu nhạy cảm (bug).
- `POST /transfer-service/internal/settle` → forward đến internal endpoint của transfer-service (bug).
- `GET /api/accounts/123` → route bình thường, không bị ảnh hưởng (không phải bug condition).

---

### Issue 3 — JWT Exception Handling Quá Rộng

**Bug Condition**: Token JWT không hợp lệ gây exception trong quá trình xác thực, nhưng tất cả exception đều bị bắt bởi `catch (Exception e)` mà không log.

```
FUNCTION isBugCondition_JwtException(X)
  INPUT: X of type JwtValidationAttempt
  OUTPUT: boolean

  RETURN X.token IS INVALID  // expired, malformed, wrong signature, null payload
         AND catch_block = "catch (Exception e)"  // không phân biệt JwtException vs SystemException
         AND log_statement EXISTS = FALSE
END FUNCTION
```

**Examples**:
- Token hết hạn (`ExpiredJwtException`) → bị bắt bởi `catch (Exception e)`, không log → không thể phát hiện tấn công replay (bug).
- Token sai chữ ký (`SignatureException`) → không log → không thể audit security event (bug).
- `NullPointerException` trong quá trình parse → bị bắt cùng với `JwtException`, không có stack trace → không thể debug (bug).
- Token hợp lệ → không vào catch block → không bị ảnh hưởng (không phải bug condition).

---

### Issue 4 — CORS Origin Hardcode

**Bug Condition**: `SecurityConfig.java` hardcode `"http://localhost:3000"` không thể override qua config hay biến môi trường.

```
FUNCTION isBugCondition_CorsHardcode(X)
  INPUT: X of type DeploymentEnvironment
  OUTPUT: boolean

  RETURN allowedOriginsSource = "HARDCODED_IN_SOURCE_CODE"
         AND X.environment IN {"docker", "prod", "staging"}
         AND X.frontendOrigin != "http://localhost:3000"
END FUNCTION
```

**Examples**:
- Deploy lên production với frontend tại `https://app.fintech.com` → CORS bị block vì origin không khớp (bug).
- Docker Compose với frontend container tại `http://frontend:3000` → CORS bị block (bug).
- Dev local với frontend tại `http://localhost:3000` → hoạt động đúng (không phải bug condition).

---

### Issue 5 — Rate Limiting Khai Báo Nhưng Không Implement

**Bug Condition**: `pom.xml` mô tả "rate limiting" nhưng không có implementation.

```
FUNCTION isBugCondition_RateLimitingMismatch(X)
  INPUT: X of type PomDescriptor
  OUTPUT: boolean

  RETURN X.description CONTAINS "rate limiting"
         AND rateLimitingImplementationExists() = FALSE
END FUNCTION
```

**Examples**:
- Developer đọc `pom.xml` và kỳ vọng có rate limiting → không tìm thấy config nào → gây nhầm lẫn (bug).
- Security audit kiểm tra rate limiting → không có implementation → false positive trong checklist (bug).

---

### Issue 6 — HA Documentation Thiếu Ví Dụ

**Bug Condition**: `application-prod.yml` có comment về HA nhưng không có ví dụ cú pháp multi-peer.

```
FUNCTION isBugCondition_HaDocMissing(X)
  INPUT: X of type ProdConfigFile
  OUTPUT: boolean

  RETURN X.hasHaComment = TRUE
         AND X.hasConcreteHaExample = FALSE
END FUNCTION
```

**Examples**:
- Operator mới cần deploy HA → đọc `application-prod.yml` → không biết cú pháp multi-peer `defaultZone` → phải tự tra cứu (bug).

---

## Expected Behavior

### Preservation Requirements

Các hành vi sau **không được thay đổi** sau khi fix:

**Unchanged Behaviors:**
- Request mang JWT token hợp lệ (đúng chữ ký, chưa hết hạn) đến protected path → vẫn được xác thực thành công, inject `X-User-Id` và `X-User-Email`, forward đến downstream.
- Request đến `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/actuator` → vẫn đi qua filter mà không yêu cầu token.
- Route `/api/accounts/**` → vẫn forward đến `account-service` qua Eureka load balancer.
- Route `/api/transfers/**` → vẫn forward đến `transfer-service`.
- Route `/api/transactions/**` → vẫn forward đến `transaction-history-service`.
- Profile `dev` → vẫn kết nối Eureka tại `http://localhost:8761/eureka/`.
- Profile `docker` → vẫn đọc `EUREKA_DEFAULT_ZONE` từ biến môi trường.
- CORS preflight (`OPTIONS`) từ origin hợp lệ → vẫn trả về response đúng với `Access-Control-Allow-Methods` và `Access-Control-Allow-Headers` hiện tại.
- Dev environment với `gateway.cors.allowed-origins` không set → fallback về `http://localhost:3000`.
- `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` → vẫn được expose theo cấu hình hiện tại.

**Scope:**
Mọi input không thỏa bất kỳ `isBugCondition` nào ở trên phải hoạt động y hệt trước khi fix.

---

## Hypothesized Root Cause

### Issue 1 — Thiếu Integration Test
- **Root Cause**: Test class chưa được tạo. Không có file `ApiGatewayApplicationTests.java` trong `src/test/`.
- **Lưu ý kỹ thuật**: Gateway dùng Spring WebFlux (reactive), nên phải dùng `WebTestClient` thay vì `TestRestTemplate`. `TestRestTemplate` chỉ hoạt động với servlet stack (Spring MVC).

### Issue 2 — Internal Endpoint Exposure
- **Root Cause**: Discovery locator tự động tạo route cho mọi service đăng ký với Eureka, bao gồm cả path `/internal/**`. Không có route nào với predicate `Path=/*/internal/**` được cấu hình để chặn trước.
- **Cơ chế**: Spring Cloud Gateway xử lý route theo thứ tự — route có `order` thấp hơn được ưu tiên. Cần thêm route blocking với order cao nhất (số âm nhỏ nhất hoặc đặt trước trong danh sách).

### Issue 3 — JWT Exception Handling
- **Root Cause**: `catch (Exception e)` trong `JwtAuthenticationFilter.filter()` bắt tất cả exception mà không phân biệt `JwtException` (lỗi token bình thường) với các lỗi hệ thống khác. Không có `Logger` được khai báo trong class.

### Issue 4 — CORS Hardcode
- **Root Cause**: `SecurityConfig.java` dùng `List.of("http://localhost:3000")` trực tiếp trong code, không inject từ `@Value` hay `@ConfigurationProperties`. Không có property `gateway.cors.allowed-origins` trong `application.yml`.

### Issue 5 — Rate Limiting Mismatch
- **Root Cause**: Description trong `pom.xml` được viết với ý định implement rate limiting nhưng chưa được implement (không có Redis, không có `RequestRateLimiter` filter). Quyết định: xóa "rate limiting" khỏi description thay vì implement (không có Redis trong stack hiện tại).

### Issue 6 — HA Documentation
- **Root Cause**: Comment `# Point to multiple Eureka peers for HA` trong `application-prod.yml` không kèm ví dụ cú pháp cụ thể.

---

## Correctness Properties

Property 1: Bug Condition — Integration Test Coverage

_For any_ gateway module build where `isBugCondition_NoTest` holds (không có test class), the fix
SHALL tạo `ApiGatewayApplicationTests` với `@SpringBootTest(webEnvironment = RANDOM_PORT)` và
`@ActiveProfiles("dev")` sử dụng `WebTestClient`, xác nhận: context loads, health endpoint trả UP,
JWT filter từ chối request không có token với 401, public path cho phép request không có token.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

---

Property 2: Bug Condition — Internal Endpoint Blocking

_For any_ HTTP request where `isBugCondition_InternalExposed` holds (path khớp `/*/internal/**`
hoặc `/internal/**`), the fixed gateway SHALL trả về HTTP 404 và KHÔNG forward request đến
downstream service.

**Validates: Requirements 2.5, 2.6**

---

Property 3: Bug Condition — JWT Exception Logging

_For any_ JWT validation attempt where `isBugCondition_JwtException` holds (token không hợp lệ),
the fixed `JwtAuthenticationFilter` SHALL: (a) log `WARN` với message mô tả lỗi nếu là
`JwtException`, (b) log `ERROR` với stack trace nếu là exception khác, (c) trả về HTTP 401 trong
cả hai trường hợp.

**Validates: Requirements 2.7, 2.8**

---

Property 4: Bug Condition — CORS Origin Configurable

_For any_ deployment environment where `isBugCondition_CorsHardcode` holds (môi trường không phải
dev với frontend origin khác `localhost:3000`), the fixed `SecurityConfig` SHALL đọc allowed origins
từ property `gateway.cors.allowed-origins` thay vì hardcode, hỗ trợ nhiều origins phân cách bằng
dấu phẩy.

**Validates: Requirements 2.9, 2.10**

---

Property 5: Preservation — Valid JWT Flow Unchanged

_For any_ request where the bug condition does NOT hold (token hợp lệ, path bình thường, không phải
internal path), the fixed gateway SHALL produce the same result as the original: xác thực thành
công, inject headers, forward đến đúng downstream service.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**

---

## Fix Implementation

### Change 1 — Tạo Integration Test

**File**: `api-gateway/src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java`

**Action**: Tạo mới

**Specific Changes**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() { /* verifies context starts without exception */ }

    @Test
    void healthEndpointReturnsUp() {
        webTestClient.get().uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).value(body -> assertThat(body).contains("\"status\":\"UP\""));
    }

    @Test
    void jwtFilterRejectsUnauthorized() {
        webTestClient.get().uri("/api/accounts/123")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void publicPathAllowsNoToken() {
        // POST /api/auth/login không có token → không bị filter chặn (có thể 404/502 từ downstream, không phải 401)
        webTestClient.post().uri("/api/auth/login")
            .exchange()
            .expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
    }
}
```

**Lưu ý**: Gateway là reactive (WebFlux) — `WebTestClient` được auto-configured bởi
`@SpringBootTest(webEnvironment = RANDOM_PORT)` khi có `spring-boot-starter-test` và WebFlux trên
classpath. Không cần thêm dependency.

---

### Change 2 — Block Internal Endpoints

**File**: `api-gateway/src/main/resources/application.yml`

**Action**: Thêm route mới vào đầu danh sách `spring.cloud.gateway.routes`

**Specific Changes**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        # SECURITY: Block all internal endpoints — must be first route
        - id: block-internal-endpoints
          uri: no://op
          predicates:
            - Path=/*/internal/**,/internal/**
          filters:
            - SetStatus=404
          order: -100  # Highest priority — processed before all other routes

        # ... existing routes unchanged ...
```

**Lưu ý**: `uri: no://op` là placeholder — request sẽ bị chặn bởi `SetStatus=404` filter trước khi
đến upstream. `order: -100` đảm bảo route này được xử lý trước discovery locator routes.

---

### Change 3 — JWT Exception Handling

**File**: `api-gateway/src/main/java/com/fintech/gateway/filter/JwtAuthenticationFilter.java`

**Action**: Sửa đổi

**Specific Changes**:
1. Thêm `Logger` field:
   ```java
   private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
   ```
2. Thêm import: `import io.jsonwebtoken.JwtException;`, `import org.slf4j.Logger;`,
   `import org.slf4j.LoggerFactory;`
3. Tách `catch (Exception e)` thành hai block:
   ```java
   } catch (JwtException e) {
       log.warn("JWT validation failed: {}", e.getMessage());
       exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
       return exchange.getResponse().setComplete();
   } catch (Exception e) {
       log.error("Unexpected error during JWT validation", e);
       exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
       return exchange.getResponse().setComplete();
   }
   ```

**Không thay đổi**: Response code (vẫn 401 cho cả hai), logic xác thực token, PUBLIC_PATHS list,
header injection logic.

---

### Change 4 — CORS Origin Configurable

**File 1**: `api-gateway/src/main/java/com/fintech/gateway/config/SecurityConfig.java`

**Action**: Sửa đổi

**Specific Changes**:
```java
@Configuration
public class SecurityConfig {

    @Value("${gateway.cors.allowed-origins:http://localhost:3000}")
    private String allowedOriginsConfig;

    @Bean
    public CorsWebFilter corsWebFilter() {
        List<String> origins = Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        // ... rest unchanged (methods, headers, credentials, maxAge) ...
    }
}
```

**File 2**: `api-gateway/src/main/resources/application.yml`

**Action**: Thêm property

```yaml
gateway:
  cors:
    allowed-origins: ${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

**Không thay đổi**: `allowedMethods`, `allowedHeaders`, `allowCredentials`, `maxAge`.

---

### Change 5 — Xóa Rate Limiting Khỏi pom.xml Description

**File**: `api-gateway/pom.xml`

**Action**: Sửa `<description>`

```xml
<!-- Before -->
<description>Spring Cloud Gateway — routing, JWT validation, rate limiting</description>

<!-- After -->
<description>Spring Cloud Gateway — routing, JWT validation, CORS</description>
```

---

### Change 6 — HA Documentation

**File**: `api-gateway/src/main/resources/application-prod.yml`

**Action**: Thêm comment ví dụ

```yaml
eureka:
  client:
    service-url:
      # Point to multiple Eureka peers for HA. Example:
      # defaultZone: http://user:pass@eureka1:8761/eureka/,http://user:pass@eureka2:8761/eureka/
      defaultZone: ${EUREKA_DEFAULT_ZONE}
```

---

## Testing Strategy

### Validation Approach

Testing theo hai phase:
1. **Exploratory**: Chạy test trên code **chưa fix** để xác nhận bug tồn tại và hiểu root cause.
2. **Fix + Preservation Checking**: Chạy test trên code **đã fix** để xác nhận fix đúng và không có regression.

---

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples trên unfixed code. Xác nhận hoặc bác bỏ root cause analysis.

**Test Plan**: Viết test trước khi fix, chạy trên code gốc để quan sát failure.

**Test Cases**:

1. **No Test Class** (Issue 1): Kiểm tra `src/test/` — không có `ApiGatewayApplicationTests.java`
   → xác nhận bug condition thỏa.

2. **Internal Path Forwarded** (Issue 2): Gửi `GET /account-service/internal/test` đến gateway
   đang chạy → quan sát response (kỳ vọng: 404 từ gateway, thực tế: forward đến downstream hoặc
   503 nếu service không chạy, nhưng không phải 404 từ gateway layer).

3. **JWT Exception Not Logged** (Issue 3): Gửi request với token hết hạn → kiểm tra log output →
   không có dòng log nào từ `JwtAuthenticationFilter`.

4. **CORS Hardcode** (Issue 4): Khởi động gateway với biến môi trường
   `GATEWAY_CORS_ALLOWED_ORIGINS=https://app.fintech.com` → gửi CORS preflight từ origin đó →
   quan sát response bị block (vì config vẫn hardcode `localhost:3000`).

**Expected Counterexamples**:
- Issue 2: Gateway không trả 404 cho `/*/internal/**` — discovery locator forward request.
- Issue 3: Không có log entry khi token invalid — `catch (Exception e)` không log.
- Issue 4: CORS bị block dù set env var — `SecurityConfig` không đọc từ property.

---

### Fix Checking

**Goal**: Xác nhận với mọi input thỏa bug condition, fixed code cho kết quả đúng.

**Pseudocode**:
```
// Issue 2
FOR ALL request WHERE isBugCondition_InternalExposed(request) DO
  result := gateway'(request)
  ASSERT result.statusCode = 404
  ASSERT result.forwardedToDownstream = FALSE
END FOR

// Issue 3
FOR ALL attempt WHERE isBugCondition_JwtException(attempt) DO
  result := validateJwt'(attempt)
  ASSERT result.statusCode = 401
  IF attempt.exceptionType IS JwtException THEN
    ASSERT log.contains(WARN, attempt.exceptionMessage)
  ELSE
    ASSERT log.contains(ERROR, stackTrace)
  END IF
END FOR

// Issue 4
FOR ALL env WHERE isBugCondition_CorsHardcode(env) DO
  result := corsConfig'(env)
  ASSERT result.allowedOrigins = env.configuredOrigins
END FOR
```

**Test Cases**:
1. `GET /account-service/internal/accounts` → expect 404 (không forward).
2. `GET /transfer-service/internal/settle` → expect 404.
3. `GET /internal/admin` → expect 404.
4. Request với expired token → expect 401 + WARN log chứa "JWT validation failed".
5. Request với malformed token → expect 401 + WARN log.
6. Gateway start với `GATEWAY_CORS_ALLOWED_ORIGINS=https://app.fintech.com` → CORS preflight từ
   `https://app.fintech.com` → expect 200 với đúng CORS headers.
7. Gateway start với multiple origins `https://app.fintech.com,https://admin.fintech.com` → cả hai
   origin đều được accept.

---

### Preservation Checking

**Goal**: Xác nhận với mọi input KHÔNG thỏa bug condition, fixed code cho kết quả y hệt code gốc.

**Pseudocode**:
```
FOR ALL request WHERE NOT isBugCondition_InternalExposed(request)
                  AND NOT isBugCondition_JwtException(request)
                  AND NOT isBugCondition_CorsHardcode(request) DO
  ASSERT F(request) = F'(request)
END FOR
```

**Testing Approach**: Property-based testing được khuyến nghị cho preservation checking vì:
- Tự động sinh nhiều test case trên input domain rộng.
- Phát hiện edge case mà unit test thủ công có thể bỏ sót.
- Đảm bảo hành vi không thay đổi trên toàn bộ non-buggy input space.

**Test Cases**:
1. **Valid JWT Preservation**: Request với token hợp lệ đến `/api/accounts/123` → expect forward
   đến downstream với `X-User-Id` và `X-User-Email` headers (y hệt trước khi fix).
2. **Public Path Preservation**: `POST /api/auth/login` không có token → expect không bị 401
   (y hệt trước khi fix).
3. **CORS Dev Preservation**: Gateway start không set `GATEWAY_CORS_ALLOWED_ORIGINS` → CORS
   preflight từ `http://localhost:3000` → expect 200 (fallback default hoạt động đúng).
4. **Non-Internal Path Preservation**: `GET /api/accounts/123` → không bị block bởi internal route.
5. **Actuator Preservation**: `GET /actuator/health` → expect 200 với `"status":"UP"`.

---

### Unit Tests

- Test `JwtAuthenticationFilter` với mock `ServerWebExchange`: expired token → 401 + WARN log.
- Test `JwtAuthenticationFilter` với mock: malformed token → 401 + WARN log.
- Test `JwtAuthenticationFilter` với mock: `NullPointerException` trong parse → 401 + ERROR log.
- Test `JwtAuthenticationFilter` với mock: valid token → headers injected, chain.filter() called.
- Test `SecurityConfig.corsWebFilter()`: single origin từ property → `allowedOrigins` đúng.
- Test `SecurityConfig.corsWebFilter()`: multiple origins phân cách phẩy → list đúng.
- Test `SecurityConfig.corsWebFilter()`: property không set → fallback `http://localhost:3000`.

---

### Property-Based Tests

- Generate random invalid JWT strings → `JwtAuthenticationFilter` luôn trả 401 và log WARN/ERROR.
- Generate random valid JWT tokens với đúng secret → filter luôn inject headers và forward.
- Generate random paths khớp `/*/internal/**` → gateway luôn trả 404.
- Generate random paths không khớp internal pattern → gateway không trả 404 từ blocking route.
- Generate random origin strings → `SecurityConfig` parse đúng từ comma-separated property.

---

### Integration Tests

- `ApiGatewayApplicationTests.contextLoads()`: context khởi động không có exception.
- `ApiGatewayApplicationTests.healthEndpointReturnsUp()`: `GET /actuator/health` → 200 + `"status":"UP"`.
- `ApiGatewayApplicationTests.jwtFilterRejectsUnauthorized()`: request không có token đến protected
  path → 401.
- `ApiGatewayApplicationTests.publicPathAllowsNoToken()`: request đến `/api/auth/login` không có
  token → không phải 401.
