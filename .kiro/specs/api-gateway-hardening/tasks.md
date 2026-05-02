# Implementation Plan

## Git Flow Convention

Mỗi task implementation (task 3–8) tuân theo Git Flow **local-only** (không push remote, không tạo PR):

```
develop
  └── bugfix/api-gateway-<issue-name>   ← tạo trước khi bắt đầu task
        └── [implement + test]
              └── merge local → develop ← squash merge sau khi hoàn thành
```

- **Base branch**: `develop`
- **Branch naming**: `bugfix/api-gateway-<issue-name>`
- **Merge strategy**: squash merge local với commit message mô tả issue đã fix
- **Không push remote, không tạo Pull Request** — tất cả thao tác git chỉ trên local
- Tasks 1–2 (exploration + preservation tests) chạy trên branch hiện tại, không cần branch riêng
- Tasks 3–8 mỗi task có branch riêng, merge độc lập vào `develop` local

---

- [x] 1. Write bug condition exploration tests (BEFORE implementing any fix)
  - **Property 1: Bug Condition** - API Gateway Hardening Issues
  - **CRITICAL**: Các test này PHẢI FAIL trên code chưa fix — failure xác nhận bug tồn tại
  - **DO NOT attempt to fix the test or the code when it fails**
  - **GOAL**: Surface counterexamples để chứng minh từng bug condition tồn tại
  - **Scoped PBT Approach**: Scope property đến các concrete failing case để đảm bảo reproducibility

  - [x] 1.1 Xác nhận thiếu integration test (Issue 1)
    - Kiểm tra `api-gateway/src/test/java/com/fintech/gateway/` — không có `ApiGatewayApplicationTests.java`
    - Xác nhận `isBugCondition_NoTest` thỏa: `testClassExists("ApiGatewayApplicationTests") = FALSE`
    - Ghi lại counterexample: "Không có integration test nào xác nhận context load, health endpoint, JWT filter"
    - **EXPECTED OUTCOME**: Bug condition thỏa (file không tồn tại)
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 1.2 Xác nhận internal endpoint bị lộ (Issue 2)
    - Gửi `GET /account-service/internal/test` đến gateway đang chạy (profile dev)
    - Quan sát response: kỳ vọng gateway KHÔNG trả 404 từ gateway layer (discovery locator forward request)
    - Gửi `GET /internal/admin` — quan sát tương tự
    - Xác nhận `isBugCondition_InternalExposed` thỏa: path khớp `^/([^/]+/)?internal(/.*)?$` không bị block
    - Ghi lại counterexample: "GET /account-service/internal/accounts không bị block bởi gateway"
    - **EXPECTED OUTCOME**: Test FAILS (gateway không trả 404 — bug tồn tại)
    - _Requirements: 2.1, 2.2_

  - [x] 1.3 Xác nhận JWT exception không được log (Issue 3)
    - Gửi request với expired JWT token đến `/api/accounts/123`
    - Gửi request với malformed JWT token (chuỗi random không phải JWT)
    - Kiểm tra log output — xác nhận không có dòng log nào từ `JwtAuthenticationFilter`
    - Xác nhận `isBugCondition_JwtException` thỏa: `catch (Exception e)` không phân biệt, không log
    - Ghi lại counterexample: "Token hết hạn → 401 nhưng không có WARN log; NullPointerException → 401 nhưng không có ERROR log với stack trace"
    - **EXPECTED OUTCOME**: Test FAILS (không có log — bug tồn tại)
    - _Requirements: 3.1, 3.2_

  - [x] 1.4 Xác nhận CORS origin bị hardcode (Issue 4)
    - Khởi động gateway với env var `GATEWAY_CORS_ALLOWED_ORIGINS=https://app.fintech.com`
    - Gửi CORS preflight `OPTIONS` từ origin `https://app.fintech.com`
    - Quan sát response: kỳ vọng bị block (vì `SecurityConfig` hardcode `localhost:3000`)
    - Xác nhận `isBugCondition_CorsHardcode` thỏa: `allowedOriginsSource = "HARDCODED_IN_SOURCE_CODE"`
    - Ghi lại counterexample: "CORS preflight từ https://app.fintech.com bị block dù đã set env var"
    - **EXPECTED OUTCOME**: Test FAILS (CORS bị block — bug tồn tại)
    - _Requirements: 4.1_

  - Mark task 1 complete khi tất cả bug conditions đã được xác nhận và counterexamples đã được ghi lại
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 3.1, 3.2, 4.1_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Existing Gateway Behavior
  - **IMPORTANT**: Follow observation-first methodology — quan sát hành vi trên UNFIXED code trước
  - Observe behavior trên code chưa fix cho các non-buggy inputs
  - Write property-based tests capturing observed behavior patterns từ Preservation Requirements
  - **EXPECTED OUTCOME**: Tests PASS trên unfixed code (xác nhận baseline behavior)
  - Mark task complete khi tests đã được viết, chạy, và pass trên unfixed code

  - [x] 2.1 Preservation: Valid JWT token flow
    - **Property 2: Preservation** - Valid JWT Token Forwarding
    - Observe: Request với valid JWT token đến `/api/accounts/123` → headers `X-User-Id` và `X-User-Email` được inject, request được forward
    - Write property-based test: for all valid JWT tokens (đúng chữ ký, chưa hết hạn), gateway inject headers và forward đến downstream
    - Verify test PASSES trên unfixed code
    - _Requirements: 3.1_

  - [x] 2.2 Preservation: Public path bypass
    - **Property 2: Preservation** - Public Path No-Auth
    - Observe: `POST /api/auth/login` không có token → không bị 401 (filter bypass)
    - Observe: `GET /api/auth/register`, `/api/auth/refresh`, `/actuator` → không bị 401
    - Write property-based test: for all paths in PUBLIC_PATHS list, request không có token không bị filter chặn
    - Verify test PASSES trên unfixed code
    - _Requirements: 3.2_

  - [x] 2.3 Preservation: Existing routes routing
    - **Property 2: Preservation** - Route Forwarding Unchanged
    - Observe: `GET /api/accounts/**` → forward đến `account-service` qua Eureka LB
    - Observe: `GET /api/transfers/**` → forward đến `transfer-service`
    - Observe: `GET /api/transactions/**` → forward đến `transaction-history-service`
    - Write property-based test: for all non-internal paths matching existing routes, routing behavior unchanged
    - Verify test PASSES trên unfixed code
    - _Requirements: 3.3_

  - [x] 2.4 Preservation: Non-internal path not blocked
    - **Property 2: Preservation** - Non-Internal Path Passthrough
    - Observe: `GET /api/accounts/123` → không bị block bởi internal route (không trả 404 từ blocking rule)
    - Write property-based test: for all paths NOT matching `^/([^/]+/)?internal(/.*)?$`, gateway không trả 404 từ block-internal-endpoints route
    - Verify test PASSES trên unfixed code
    - _Requirements: 3.3_

  - [x] 2.5 Preservation: CORS dev environment fallback
    - **Property 2: Preservation** - CORS Default Origin
    - Observe: Gateway start không set `GATEWAY_CORS_ALLOWED_ORIGINS` → CORS preflight từ `http://localhost:3000` → 200 với đúng CORS headers
    - Write test: khi property không set, fallback về `http://localhost:3000` hoạt động đúng
    - Verify test PASSES trên unfixed code
    - _Requirements: 3.7_

  - [x] 2.6 Preservation: Actuator endpoints exposed
    - **Property 2: Preservation** - Actuator Availability
    - Observe: `GET /actuator/health` → 200 với `"status":"UP"`
    - Observe: `/actuator/metrics`, `/actuator/prometheus` → accessible theo config hiện tại
    - Write test: actuator endpoints vẫn được expose sau khi fix
    - Verify test PASSES trên unfixed code
    - _Requirements: 3.6_

  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [ ] 3. Fix Issue 1 — Tạo Integration Test

  - [x] 3.0 Tạo Git branch cho task này
    - Đảm bảo đang ở branch `develop`: `git checkout develop`
    - Tạo branch mới: `git checkout -b bugfix/api-gateway-integration-test`
    - Xác nhận branch hiện tại: `git branch --show-current`

  - [x] 3.1 Tạo file `ApiGatewayApplicationTests.java`
    - Tạo `api-gateway/src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java`
    - Annotate với `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`
    - Annotate với `@ActiveProfiles("dev")`
    - Inject `@Autowired WebTestClient webTestClient` (reactive stack — KHÔNG dùng `TestRestTemplate`)
    - _Bug_Condition: isBugCondition_NoTest — testClassExists("ApiGatewayApplicationTests") = FALSE_
    - _Expected_Behavior: Test class tồn tại với 4 test methods xác nhận context load, health, JWT filter, public path_
    - _Preservation: Không thay đổi routing logic, filter logic, hay bất kỳ production code nào_
    - _Requirements: 2.1_
    - `git add api-gateway/src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java`
    - `git commit -m "test(api-gateway): create ApiGatewayApplicationTests skeleton"`

  - [x] 3.2 Implement test method `contextLoads()`
    - `@Test void contextLoads()` — verifies Spring context khởi động không có exception
    - Method body rỗng là đủ (context load failure sẽ throw exception trước khi method chạy)
    - _Requirements: 2.1_
    - `git add api-gateway/src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java`
    - `git commit -m "test(api-gateway): implement contextLoads test"`

  - [x] 3.3 Implement test method `healthEndpointReturnsUp()`
    - `@Test void healthEndpointReturnsUp()`
    - `webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk()`
    - Assert body chứa `"status":"UP"` dùng `.expectBody(String.class).value(body -> assertThat(body).contains("\"status\":\"UP\""))`
    - _Requirements: 2.2_
    - `git add api-gateway/src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java`
    - `git commit -m "test(api-gateway): implement healthEndpointReturnsUp test"`

  - [x] 3.4 Implement test method `jwtFilterRejectsUnauthorized()`
    - `@Test void jwtFilterRejectsUnauthorized()`
    - `webTestClient.get().uri("/api/accounts/123").exchange().expectStatus().isUnauthorized()`
    - Không gửi `Authorization` header — xác nhận `JwtAuthenticationFilter` trả 401
    - _Requirements: 2.3_
    - `git add api-gateway/src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java`
    - `git commit -m "test(api-gateway): implement jwtFilterRejectsUnauthorized test"`

  - [x] 3.5 Implement test method `publicPathAllowsNoToken()`
    - `@Test void publicPathAllowsNoToken()`
    - `webTestClient.post().uri("/api/auth/login").exchange().expectStatus().value(status -> assertThat(status).isNotEqualTo(401))`
    - Response có thể là 404/502 từ downstream (service không chạy trong test) — quan trọng là KHÔNG phải 401
    - _Requirements: 2.4_
    - `git add api-gateway/src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java`
    - `git commit -m "test(api-gateway): implement publicPathAllowsNoToken test"`

  - [x] 3.6 Verify **Property 1: Expected Behavior** — Integration test passes
    - **Property 1: Expected Behavior** - Integration Test Coverage
    - **IMPORTANT**: Re-run the SAME exploration check từ task 1.1 — KHÔNG viết test mới
    - Xác nhận `ApiGatewayApplicationTests.java` tồn tại và compile thành công
    - Chạy: `mvn test -pl api-gateway -Dtest=ApiGatewayApplicationTests`
    - **EXPECTED OUTCOME**: Test PASSES — bug condition không còn thỏa (file tồn tại)
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 3.7 Verify **Property 2: Preservation** — Existing behavior unchanged
    - **Property 2: Preservation** - No Regression from Test Addition
    - **IMPORTANT**: Re-run preservation tests từ task 2 — KHÔNG viết test mới
    - Xác nhận việc thêm test class không ảnh hưởng đến production code
    - Chạy full test suite: `mvn test -pl api-gateway`
    - **EXPECTED OUTCOME**: Tất cả preservation tests PASS

  - [x] 3.8 Merge local vào `develop`
    - Chuyển về `develop`: `git checkout develop`
    - Squash merge: `git merge --squash bugfix/api-gateway-integration-test`
    - Commit merge: `git commit -m "fix(api-gateway): add integration tests for context load, health, JWT filter, public path"`
    - **⚠️ DỪNG LẠI — Yêu cầu xác nhận**: Báo cáo kết quả merge cho user
    - **KHÔNG tiếp tục task 4 cho đến khi nhận được xác nhận từ user**

- [ ] 4. Fix Issue 2 — Block Internal Endpoints

  - [x] 4.0 Tạo Git branch cho task này
    - Đảm bảo đang ở branch `develop` (sau khi merge task 3): `git checkout develop`
    - Tạo branch mới: `git checkout -b bugfix/api-gateway-block-internal-endpoints`
    - Xác nhận branch hiện tại: `git branch --show-current`

  - [x] 4.1 Thêm route `block-internal-endpoints` vào `application.yml`
    - Mở `api-gateway/src/main/resources/application.yml`
    - Thêm route mới vào **đầu** danh sách `spring.cloud.gateway.routes` (trước tất cả routes hiện tại):
      ```yaml
      # SECURITY: Block all internal endpoints — must be first route
      - id: block-internal-endpoints
        uri: no://op
        predicates:
          - Path=/*/internal/**,/internal/**
        filters:
          - SetStatus=404
        order: -100
      ```
    - `order: -100` đảm bảo route được xử lý trước discovery locator routes
    - `uri: no://op` là placeholder — request bị chặn bởi `SetStatus=404` trước khi đến upstream
    - _Bug_Condition: isBugCondition_InternalExposed — path MATCHES "^/([^/]+/)?internal(/.*)?$"_
    - _Expected_Behavior: result.statusCode = 404 AND result NOT forwarded to downstream_
    - _Preservation: Routes /api/accounts/**, /api/transfers/**, /api/transactions/** không bị ảnh hưởng_
    - _Requirements: 2.5, 2.6_
    - `git add api-gateway/src/main/resources/application.yml`
    - `git commit -m "fix(api-gateway): block internal endpoints via gateway route (order -100, SetStatus 404)"`

  - [x] 4.2 Verify **Property 1: Expected Behavior** — Internal paths blocked
    - **Property 1: Expected Behavior** - Internal Endpoint Blocking
    - **IMPORTANT**: Re-run exploration tests từ task 1.2 — KHÔNG viết test mới
    - Test: `GET /account-service/internal/accounts` → expect 404
    - Test: `GET /transfer-service/internal/settle` → expect 404
    - Test: `GET /internal/admin` → expect 404
    - **EXPECTED OUTCOME**: Tests PASS — gateway trả 404 cho tất cả internal paths
    - _Requirements: 2.5, 2.6_

  - [x] 4.3 Verify **Property 2: Preservation** — Normal routing unchanged
    - **Property 2: Preservation** - Non-Internal Path Passthrough
    - **IMPORTANT**: Re-run preservation tests từ task 2.3 và 2.4 — KHÔNG viết test mới
    - Test: `GET /api/accounts/123` → không bị block (không trả 404 từ blocking route)
    - Test: `GET /api/transfers/456` → route bình thường
    - **EXPECTED OUTCOME**: Preservation tests PASS — không có regression

  - [x] 4.4 Merge local vào `develop`
    - Chuyển về `develop`: `git checkout develop`
    - Squash merge: `git merge --squash bugfix/api-gateway-block-internal-endpoints`
    - Commit merge: `git commit -m "fix(api-gateway): block internal endpoints via gateway route (order -100, SetStatus 404)"`
    - **⚠️ DỪNG LẠI — Yêu cầu xác nhận**: Báo cáo kết quả merge cho user
    - **KHÔNG tiếp tục task 5 cho đến khi nhận được xác nhận từ user**

- [ ] 5. Fix Issue 3 — JWT Exception Handling

  - [x] 5.0 Tạo Git branch cho task này
    - Đảm bảo đang ở branch `develop` (sau khi merge task 4): `git checkout develop`
    - Tạo branch mới: `git checkout -b bugfix/api-gateway-jwt-exception-handling`
    - Xác nhận branch hiện tại: `git branch --show-current`

  - [x] 5.1 Thêm SLF4J Logger vào `JwtAuthenticationFilter`
    - Mở `api-gateway/src/main/java/com/fintech/gateway/filter/JwtAuthenticationFilter.java`
    - Thêm import: `import org.slf4j.Logger;` và `import org.slf4j.LoggerFactory;`
    - Thêm field: `private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);`
    - _Requirements: 2.7, 2.8_
    - `git add api-gateway/src/main/java/com/fintech/gateway/filter/JwtAuthenticationFilter.java`
    - `git commit -m "fix(api-gateway): add SLF4J logger to JwtAuthenticationFilter"`

  - [x] 5.2 Tách `catch (Exception e)` thành hai catch blocks
    - Thêm import: `import io.jsonwebtoken.JwtException;`
    - Thay thế `catch (Exception e)` hiện tại bằng:
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
    - `JwtException` bắt: `ExpiredJwtException`, `MalformedJwtException`, `SignatureException` và các subclass
    - `Exception` bắt: `NullPointerException`, `IllegalStateException` và các lỗi hệ thống khác
    - Response code vẫn là 401 cho cả hai trường hợp (không thay đổi behavior)
    - _Bug_Condition: isBugCondition_JwtException — token IS INVALID AND catch_block = "catch (Exception e)" AND log_statement EXISTS = FALSE_
    - _Expected_Behavior: JwtException → WARN log + 401; SystemException → ERROR log với stack trace + 401_
    - _Preservation: Valid token flow không bị ảnh hưởng; PUBLIC_PATHS list không thay đổi; header injection logic không thay đổi_
    - _Requirements: 2.7, 2.8_
    - `git add api-gateway/src/main/java/com/fintech/gateway/filter/JwtAuthenticationFilter.java`
    - `git commit -m "fix(api-gateway): distinguish JwtException (WARN) from system exceptions (ERROR) with SLF4J logging"`

  - [x] 5.3 Verify **Property 1: Expected Behavior** — JWT exceptions logged correctly
    - **Property 1: Expected Behavior** - JWT Exception Logging
    - **IMPORTANT**: Re-run exploration tests từ task 1.3 — KHÔNG viết test mới
    - Test: expired token → expect 401 + WARN log chứa "JWT validation failed"
    - Test: malformed token → expect 401 + WARN log
    - Test: simulate `NullPointerException` trong parse → expect 401 + ERROR log với stack trace
    - **EXPECTED OUTCOME**: Tests PASS — exceptions được log đúng level
    - _Requirements: 2.7, 2.8_

  - [x] 5.4 Verify **Property 2: Preservation** — Valid JWT flow unchanged
    - **Property 2: Preservation** - Valid JWT Token Forwarding
    - **IMPORTANT**: Re-run preservation tests từ task 2.1 — KHÔNG viết test mới
    - Test: valid JWT token → headers injected, chain.filter() called, không vào catch block
    - **EXPECTED OUTCOME**: Preservation tests PASS — không có regression

  - [x] 5.5 Merge local vào `develop`
    - Chuyển về `develop`: `git checkout develop`
    - Squash merge: `git merge --squash bugfix/api-gateway-jwt-exception-handling`
    - Commit merge: `git commit -m "fix(api-gateway): distinguish JwtException (WARN) from system exceptions (ERROR) with SLF4J logging"`
    - **⚠️ DỪNG LẠI — Yêu cầu xác nhận**: Báo cáo kết quả merge cho user
    - **KHÔNG tiếp tục task 6 cho đến khi nhận được xác nhận từ user**

- [x] 6. Fix Issue 4 — CORS Origin Configurable

  - [x] 6.0 Tạo Git branch cho task này
    - Đảm bảo đang ở branch `develop` (sau khi merge task 5): `git checkout develop`
    - Tạo branch mới: `git checkout -b bugfix/api-gateway-cors-configurable`
    - Xác nhận branch hiện tại: `git branch --show-current`

  - [x] 6.1 Sửa `SecurityConfig.java` để inject allowed origins từ property
    - Mở `api-gateway/src/main/java/com/fintech/gateway/config/SecurityConfig.java`
    - Thêm import: `import org.springframework.beans.factory.annotation.Value;`, `import java.util.Arrays;`, `import java.util.stream.Collectors;`
    - Thêm field: `@Value("${gateway.cors.allowed-origins:http://localhost:3000}") private String allowedOriginsConfig;`
    - Trong `corsWebFilter()`, thay `List.of("http://localhost:3000")` bằng:
      ```java
      List<String> origins = Arrays.stream(allowedOriginsConfig.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .collect(Collectors.toList());
      ```
    - Giữ nguyên `allowedMethods`, `allowedHeaders`, `allowCredentials`, `maxAge`
    - _Bug_Condition: isBugCondition_CorsHardcode — allowedOriginsSource = "HARDCODED_IN_SOURCE_CODE"_
    - _Expected_Behavior: result.allowedOrigins = resolveFromProperty(environment)_
    - _Preservation: Dev environment với property không set → fallback về http://localhost:3000_
    - _Requirements: 2.9, 2.10_
    - `git add api-gateway/src/main/java/com/fintech/gateway/config/SecurityConfig.java`
    - `git commit -m "fix(api-gateway): inject CORS allowed-origins from property in SecurityConfig"`

  - [x] 6.2 Thêm property `gateway.cors.allowed-origins` vào `application.yml`
    - Mở `api-gateway/src/main/resources/application.yml`
    - Thêm section mới ở cuối file:
      ```yaml
      gateway:
        cors:
          allowed-origins: ${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000}
      ```
    - Giá trị mặc định `http://localhost:3000` đảm bảo dev environment không bị ảnh hưởng
    - _Requirements: 2.9, 2.10_
    - `git add api-gateway/src/main/resources/application.yml`
    - `git commit -m "fix(api-gateway): add gateway.cors.allowed-origins property with env var support"`

  - [x] 6.3 Verify **Property 1: Expected Behavior** — CORS configurable
    - **Property 1: Expected Behavior** - CORS Origin Configurable
    - **IMPORTANT**: Re-run exploration tests từ task 1.4 — KHÔNG viết test mới
    - Test: gateway start với `GATEWAY_CORS_ALLOWED_ORIGINS=https://app.fintech.com` → CORS preflight từ origin đó → expect 200 với đúng CORS headers
    - Test: multiple origins `https://app.fintech.com,https://admin.fintech.com` → cả hai origin được accept
    - **EXPECTED OUTCOME**: Tests PASS — CORS origins được đọc từ property
    - _Requirements: 2.9, 2.10_

  - [x] 6.4 Verify **Property 2: Preservation** — Dev CORS fallback unchanged
    - **Property 2: Preservation** - CORS Default Origin
    - **IMPORTANT**: Re-run preservation tests từ task 2.5 — KHÔNG viết test mới
    - Test: gateway start không set `GATEWAY_CORS_ALLOWED_ORIGINS` → CORS preflight từ `http://localhost:3000` → expect 200
    - **EXPECTED OUTCOME**: Preservation tests PASS — dev environment không bị ảnh hưởng

  - [x] 6.5 Merge local vào `develop`
    - Chuyển về `develop`: `git checkout develop`
    - Squash merge: `git merge --squash bugfix/api-gateway-cors-configurable`
    - Commit merge: `git commit -m "fix(api-gateway): externalize CORS allowed-origins via GATEWAY_CORS_ALLOWED_ORIGINS env var"`
    - **⚠️ DỪNG LẠI — Yêu cầu xác nhận**: Báo cáo kết quả merge cho user
    - **KHÔNG tiếp tục task 7 cho đến khi nhận được xác nhận từ user**

- [-] 7. Fix Issue 5 — Xóa Rate Limiting Khỏi pom.xml Description

  - [x] 7.0 Tạo Git branch cho task này
    - Đảm bảo đang ở branch `develop` (sau khi merge task 6): `git checkout develop`
    - Tạo branch mới: `git checkout -b bugfix/api-gateway-pom-description`
    - Xác nhận branch hiện tại: `git branch --show-current`

  - [x] 7.1 Sửa `<description>` trong `api-gateway/pom.xml`
    - Mở `api-gateway/pom.xml`
    - Thay đổi:
      ```xml
      <!-- Before -->
      <description>Spring Cloud Gateway — routing, JWT validation, rate limiting</description>

      <!-- After -->
      <description>Spring Cloud Gateway — routing, JWT validation, CORS</description>
      ```
    - Xóa "rate limiting" vì không có implementation (không có Redis, không có `RequestRateLimiter` filter)
    - Thêm "CORS" để phản ánh đúng tính năng thực tế
    - _Bug_Condition: isBugCondition_RateLimitingMismatch — description CONTAINS "rate limiting" AND rateLimitingImplementationExists() = FALSE_
    - _Expected_Behavior: description phản ánh đúng tính năng thực tế_
    - _Requirements: 2.11_
    - `git add api-gateway/pom.xml`
    - `git commit -m "fix(api-gateway): remove unimplemented rate limiting from pom description"`

  - [x] 7.2 Merge local vào `develop`
    - Chuyển về `develop`: `git checkout develop`
    - Squash merge: `git merge --squash bugfix/api-gateway-pom-description`
    - Commit merge: `git commit -m "fix(api-gateway): remove unimplemented rate limiting from pom description"`
    - **⚠️ DỪNG LẠI — Yêu cầu xác nhận**: Báo cáo kết quả merge cho user
    - **KHÔNG tiếp tục task 8 cho đến khi nhận được xác nhận từ user**

- [x] 8. Fix Issue 6 — HA Documentation

  - [x] 8.0 Tạo Git branch cho task này
    - Đảm bảo đang ở branch `develop` (sau khi merge task 7): `git checkout develop`
    - Tạo branch mới: `git checkout -b bugfix/api-gateway-ha-docs`
    - Xác nhận branch hiện tại: `git branch --show-current`

  - [x] 8.1 Thêm comment ví dụ multi-peer vào `application-prod.yml`
    - Mở `api-gateway/src/main/resources/application-prod.yml`
    - Cập nhật section Eureka client:
      ```yaml
      eureka:
        client:
          service-url:
            # Point to multiple Eureka peers for HA. Example:
            # defaultZone: http://user:pass@eureka1:8761/eureka/,http://user:pass@eureka2:8761/eureka/
            defaultZone: ${EUREKA_DEFAULT_ZONE}
      ```
    - Giữ nguyên `defaultZone: ${EUREKA_DEFAULT_ZONE}` — không thay đổi runtime behavior
    - _Bug_Condition: isBugCondition_HaDocMissing — hasHaComment = TRUE AND hasConcreteHaExample = FALSE_
    - _Expected_Behavior: comment có ví dụ cú pháp multi-peer cụ thể_
    - _Requirements: 2.12_
    - `git add api-gateway/src/main/resources/application-prod.yml`
    - `git commit -m "docs(api-gateway): add multi-peer Eureka HA example in application-prod.yml"`

  - [x] 8.2 Merge local vào `develop`
    - Chuyển về `develop`: `git checkout develop`
    - Squash merge: `git merge --squash bugfix/api-gateway-ha-docs`
    - Commit merge: `git commit -m "docs(api-gateway): add multi-peer Eureka HA example in application-prod.yml"`
    - **⚠️ DỪNG LẠI — Yêu cầu xác nhận**: Báo cáo kết quả merge cho user
    - **KHÔNG tiếp tục task 9 cho đến khi nhận được xác nhận từ user**

- [x] 9. Checkpoint — Ensure all tests pass
  - Đảm bảo đang ở branch `develop` với tất cả 6 bugfix branches đã được merge local
  - Chạy full test suite: `mvn test -pl api-gateway`
  - Xác nhận tất cả 4 integration tests trong `ApiGatewayApplicationTests` PASS
  - Xác nhận tất cả preservation tests PASS (không có regression)
  - Xác nhận exploration tests từ task 1 giờ PASS (bugs đã được fix)
  - Review tất cả 6 changes đã được implement đúng theo design
  - Nếu có test fail, điều tra và fix trước khi đánh dấu complete
  - **Git Flow summary**: Xác nhận tất cả branches đã được squash merge vào `develop` local:
    - `bugfix/api-gateway-integration-test` ✓
    - `bugfix/api-gateway-block-internal-endpoints` ✓
    - `bugfix/api-gateway-jwt-exception-handling` ✓
    - `bugfix/api-gateway-cors-configurable` ✓
    - `bugfix/api-gateway-pom-description` ✓
    - `bugfix/api-gateway-ha-docs` ✓
  - Kiểm tra git log: `git log --oneline develop` để xác nhận 6 commit fix đã có trên `develop`
