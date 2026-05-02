# Bugfix Requirements Document

## Introduction

Module `api-gateway` trong dự án fintech microservices hiện tồn đọng 6 issues ảnh hưởng đến độ an toàn, khả năng kiểm thử và khả năng vận hành của hệ thống. Các issues bao gồm: thiếu integration test, lộ internal endpoint ra ngoài qua discovery locator, xử lý exception JWT quá rộng và không có logging, CORS origin bị hardcode không thể override, thiếu implementation rate limiting dù đã khai báo trong mô tả, và thiếu tài liệu cấu hình Eureka HA. Spec này ghi nhận hành vi hiện tại (lỗi), hành vi kỳ vọng (đúng), và hành vi không được thay đổi (regression prevention).

---

## Bug Analysis

### Current Behavior (Defect)

**Section 1 — Thiếu Integration Test**

1.1 WHEN module `api-gateway` được build và deploy THEN hệ thống không có bất kỳ test nào xác nhận application context khởi động thành công.

1.2 WHEN endpoint `/actuator/health` được gọi THEN hệ thống không có test nào xác nhận response trả về `UP`.

1.3 WHEN một request không mang `Authorization` header được gửi đến protected path THEN hệ thống không có test nào xác nhận `JwtAuthenticationFilter` từ chối request với HTTP 401.

1.4 WHEN một request được gửi đến public path `/api/auth/**` mà không có token THEN hệ thống không có test nào xác nhận request được phép đi qua filter.

**Section 2 — Internal Endpoint Exposure**

2.1 WHEN Spring Cloud Gateway discovery locator được bật (`spring.cloud.gateway.discovery.locator.enabled: true`) THEN hệ thống tự động tạo route cho tất cả service đã đăng ký với Eureka, bao gồm cả path `/internal/**` của `account-service`.

2.2 WHEN một request bên ngoài gửi đến path `/account-service/internal/**` hoặc path tương đương qua discovery locator THEN hệ thống không block request mà forward thẳng đến `InternalAccountController` của `account-service`.

**Section 3 — JWT Exception Handling**

3.1 WHEN token JWT không hợp lệ (sai chữ ký, hết hạn, sai format) được gửi đến gateway THEN hệ thống bắt tất cả exception bằng `catch (Exception e)` mà không phân biệt `JwtException` (lỗi token) với các lỗi hệ thống khác (ví dụ `NullPointerException`, `IllegalStateException`).

3.2 WHEN bất kỳ exception nào xảy ra trong quá trình xác thực JWT THEN hệ thống không ghi log, khiến việc điều tra sự cố và phát hiện tấn công trở nên không thể.

**Section 4 — CORS Origin Hardcode**

4.1 WHEN gateway chạy trong môi trường docker hoặc production THEN hệ thống vẫn chỉ cho phép origin `http://localhost:3000` vì giá trị này bị hardcode trong `SecurityConfig.java` và không thể override qua biến môi trường hay config file.

**Section 5 — Rate Limiting Thiếu Implementation**

5.1 WHEN `api-gateway/pom.xml` được đọc THEN `<description>` ghi "rate limiting" nhưng không có bất kỳ implementation nào (không có `RequestRateLimiter` filter, không có Redis dependency, không có config) trong codebase.

**Section 6 — HA Setup Documentation**

6.1 WHEN `application-prod.yml` được đọc THEN hệ thống chỉ có comment `# Point to multiple Eureka peers for HA` nhưng không có ví dụ cụ thể về cú pháp multi-peer `defaultZone`, khiến operator không biết cách cấu hình đúng khi deploy HA.

---

### Expected Behavior (Correct)

**Section 1 — Integration Test**

2.1 WHEN module `api-gateway` được build THEN hệ thống SHALL có integration test class sử dụng `@SpringBootTest(webEnvironment = RANDOM_PORT)` và `@ActiveProfiles("dev")` xác nhận application context khởi động thành công mà không có exception.

2.2 WHEN integration test gọi `GET /actuator/health` THEN hệ thống SHALL trả về HTTP 200 với body chứa `"status":"UP"`.

2.3 WHEN integration test gửi request đến protected path mà không có `Authorization` header THEN hệ thống SHALL trả về HTTP 401 và `JwtAuthenticationFilter` SHALL từ chối request trước khi forward đến downstream service.

2.4 WHEN integration test gửi request đến `/api/auth/login` hoặc `/api/auth/register` mà không có token THEN hệ thống SHALL cho phép request đi qua filter mà không trả về 401.

**Section 2 — Internal Endpoint Blocking**

2.5 WHEN một request bên ngoài gửi đến bất kỳ path nào khớp với `/*/internal/**` hoặc `/internal/**` THEN hệ thống SHALL block request và trả về HTTP 404 hoặc HTTP 403 trước khi forward đến downstream service.

2.6 WHEN route blocking cho `/internal/**` được cấu hình THEN hệ thống SHALL áp dụng rule này cho tất cả service, không chỉ `account-service`, để đảm bảo bảo vệ theo chiều sâu.

**Section 3 — JWT Exception Handling**

2.7 WHEN token JWT không hợp lệ gây ra `JwtException` (bao gồm `ExpiredJwtException`, `MalformedJwtException`, `SignatureException`) THEN hệ thống SHALL log warning với message mô tả loại lỗi JWT và trả về HTTP 401.

2.8 WHEN một exception không phải `JwtException` xảy ra trong quá trình xác thực JWT THEN hệ thống SHALL log error với stack trace đầy đủ và trả về HTTP 401, phân biệt rõ với lỗi JWT thông thường.

**Section 4 — CORS Origin Configurable**

2.9 WHEN gateway khởi động THEN hệ thống SHALL đọc danh sách allowed origins từ property `gateway.cors.allowed-origins` (có thể set qua biến môi trường `GATEWAY_CORS_ALLOWED_ORIGINS`) thay vì hardcode.

2.10 WHEN property `gateway.cors.allowed-origins` không được set THEN hệ thống SHALL fallback về giá trị mặc định `http://localhost:3000` để không phá vỡ môi trường dev hiện tại.

**Section 5 — Rate Limiting hoặc Xóa Mô Tả**

2.11 WHEN `api-gateway/pom.xml` được đọc THEN `<description>` SHALL phản ánh đúng tính năng thực tế: hoặc implement rate limiting (thêm `RequestRateLimiter` filter với Redis), hoặc xóa "rate limiting" khỏi description.

**Section 6 — HA Documentation**

2.12 WHEN `application-prod.yml` được đọc THEN hệ thống SHALL có comment ví dụ cụ thể về cú pháp multi-peer Eureka `defaultZone`, ví dụ:
```
# defaultZone: http://user:pass@eureka1:8761/eureka/,http://user:pass@eureka2:8761/eureka/
```

---

### Unchanged Behavior (Regression Prevention)

**Section 3 — Hành vi không được thay đổi**

3.1 WHEN request hợp lệ mang JWT token đúng chữ ký và chưa hết hạn được gửi đến protected path THEN hệ thống SHALL CONTINUE TO xác thực token thành công, inject header `X-User-Id` và `X-User-Email`, và forward request đến downstream service.

3.2 WHEN request được gửi đến `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, hoặc `/actuator` THEN hệ thống SHALL CONTINUE TO cho phép request đi qua `JwtAuthenticationFilter` mà không yêu cầu token.

3.3 WHEN gateway nhận request cho các route đã cấu hình (`/api/accounts/**`, `/api/transfers/**`, `/api/transactions/**`) THEN hệ thống SHALL CONTINUE TO route đúng đến service tương ứng qua Eureka load balancer.

3.4 WHEN gateway chạy với profile `dev` THEN hệ thống SHALL CONTINUE TO kết nối Eureka tại `http://localhost:8761/eureka/` theo cấu hình `application-dev.yml`.

3.5 WHEN gateway chạy với profile `docker` THEN hệ thống SHALL CONTINUE TO kết nối Eureka tại địa chỉ được cấu hình trong `application-docker.yml` qua biến môi trường `EUREKA_DEFAULT_ZONE`.

3.6 WHEN `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` được gọi THEN hệ thống SHALL CONTINUE TO expose các endpoint này theo cấu hình `management.endpoints.web.exposure.include` hiện tại.

3.7 WHEN CORS preflight request (`OPTIONS`) được gửi từ origin hợp lệ THEN hệ thống SHALL CONTINUE TO trả về response CORS đúng với các header `Access-Control-Allow-Methods` và `Access-Control-Allow-Headers` hiện tại.

---

## Bug Condition Pseudocode

### Issue 1 — Thiếu Test

```pascal
FUNCTION isBugCondition_NoTest(X)
  INPUT: X of type GatewayRequest
  OUTPUT: boolean
  
  RETURN testSuiteExists(api-gateway) = FALSE
END FUNCTION

// Property: Fix Checking
FOR ALL X WHERE isBugCondition_NoTest(X) DO
  result ← runIntegrationTests'(api-gateway)
  ASSERT contextLoads(result) = TRUE
    AND healthEndpointReturnsUp(result) = TRUE
    AND unauthorizedRequestReturns401(result) = TRUE
    AND publicPathAllowsNoToken(result) = TRUE
END FOR

// Property: Preservation Checking
FOR ALL X WHERE NOT isBugCondition_NoTest(X) DO
  ASSERT F(X) = F'(X)  // existing routing behavior unchanged
END FOR
```

### Issue 2 — Internal Endpoint Exposure

```pascal
FUNCTION isBugCondition_InternalExposed(X)
  INPUT: X of type HttpRequest
  OUTPUT: boolean
  
  RETURN X.path MATCHES "^/([^/]+/)?internal/.*$"
END FUNCTION

// Property: Fix Checking
FOR ALL X WHERE isBugCondition_InternalExposed(X) DO
  result ← gateway'(X)
  ASSERT result.statusCode IN {403, 404}
    AND result NOT forwarded to downstream
END FOR

// Property: Preservation Checking
FOR ALL X WHERE NOT isBugCondition_InternalExposed(X) DO
  ASSERT F(X) = F'(X)  // normal routing unchanged
END FOR
```

### Issue 3 — JWT Exception Handling

```pascal
FUNCTION isBugCondition_JwtException(X)
  INPUT: X of type JwtValidationAttempt
  OUTPUT: boolean
  
  RETURN X.token IS INVALID  // expired, malformed, wrong signature
END FUNCTION

// Property: Fix Checking
FOR ALL X WHERE isBugCondition_JwtException(X) DO
  result ← validateJwt'(X)
  ASSERT result.statusCode = 401
    AND logContains(WARNING_OR_ERROR, X.exceptionType)
    AND exceptionTypeDistinguished(JwtException, SystemException)
END FOR

// Property: Preservation Checking
FOR ALL X WHERE NOT isBugCondition_JwtException(X) DO
  ASSERT F(X) = F'(X)  // valid token flow unchanged
END FOR
```

### Issue 4 — CORS Hardcode

```pascal
FUNCTION isBugCondition_CorsHardcode(X)
  INPUT: X of type DeploymentEnvironment
  OUTPUT: boolean
  
  RETURN X.environment IN {"docker", "prod"}
    AND X.frontendOrigin != "http://localhost:3000"
END FUNCTION

// Property: Fix Checking
FOR ALL X WHERE isBugCondition_CorsHardcode(X) DO
  result ← corsConfig'(X)
  ASSERT result.allowedOrigins = resolveFromProperty(X.environment)
END FOR

// Property: Preservation Checking
FOR ALL X WHERE NOT isBugCondition_CorsHardcode(X) DO
  ASSERT F(X) = F'(X)  // dev environment CORS unchanged
END FOR
```
