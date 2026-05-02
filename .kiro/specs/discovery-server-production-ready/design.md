# Design Document: Discovery Server Production-Ready

## Overview

Tài liệu thiết kế này mô tả kiến trúc và các thành phần kỹ thuật để đưa Discovery Server (Eureka) lên mức production-ready trong nền tảng fintech microservices. Thiết kế bao gồm: bảo mật bằng Basic Auth, nâng cấp Spring Boot 3.5.x, cấu hình đa môi trường, structured logging, High Availability với peer replication, monitoring/health check, cập nhật Docker Compose và Dockerfile, cùng integration testing.

### Nguyên tắc thiết kế

- **Nhất quán với hệ thống hiện tại**: Tái sử dụng các pattern đã có trong project (logback-spring.xml, actuator config, SecurityFilterChain) thay vì đưa vào pattern mới.
- **Cấu hình bên ngoài (Externalized Configuration)**: Mọi thông tin nhạy cảm (credentials) đều đọc từ environment variables, không hardcode.
- **Tối thiểu hóa thay đổi**: Chỉ thêm dependencies và cấu hình cần thiết, không over-engineer.
- **Production-parity**: Môi trường docker-compose phải gần giống production nhất có thể (HA, security, monitoring).

### Phạm vi thay đổi

| Thành phần | Thay đổi |
|---|---|
| `pom.xml` (parent) | Nâng cấp Spring Boot 3.5.x, Spring Cloud 2025.0.0 |
| `discovery-server/pom.xml` | Thêm dependencies: spring-boot-starter-security, spring-boot-starter-actuator, micrometer-registry-prometheus, logstash-logback-encoder, spring-boot-starter-test |
| `SecurityConfig.java` | Tạo mới - cấu hình Basic Auth, CSRF disable cho Eureka API |
| `application.yml` | Cấu hình chung (actuator, security credentials, default profile) |
| `application-dev.yml` | Tạo mới - standalone mode, localhost |
| `application-docker.yml` | Tạo mới - HA mode, peer replication |
| `application-prod.yml` | Tạo mới - HA mode, production settings |
| `logback-spring.xml` | Tạo mới - structured logging (JSON cho non-dev, human-readable cho dev) |
| `Dockerfile` | Cập nhật - thêm profile support, healthcheck |
| `docker-compose.yml` | Cập nhật - 2 discovery instances, health checks, credentials |
| `prometheus.yml` | Cập nhật - thêm scrape config cho discovery servers |
| Integration tests | Tạo mới - test context startup, health, dashboard, API |
| `README.md` | Tạo mới - documentation |

## Architecture

### Kiến trúc tổng quan

```mermaid
graph TB
    subgraph "Docker Compose Environment"
        subgraph "Discovery Cluster (HA)"
            DS1[discovery-server-1<br/>:8761]
            DS2[discovery-server-2<br/>:8762]
            DS1 <-->|peer replication| DS2
        end

        subgraph "Service Clients"
            GW[api-gateway<br/>:8080]
            AS[account-service<br/>:8081]
            TS[transfer-service<br/>:8082]
            THS[transaction-history-service<br/>:8083]
        end

        subgraph "Monitoring"
            PROM[Prometheus<br/>:9090]
            GRAF[Grafana<br/>:3000]
        end

        GW -->|register + discover| DS1
        GW -->|register + discover| DS2
        AS -->|register + discover| DS1
        AS -->|register + discover| DS2
        TS -->|register + discover| DS1
        TS -->|register + discover| DS2
        THS -->|register + discover| DS1
        THS -->|register + discover| DS2

        PROM -->|scrape /actuator/prometheus| DS1
        PROM -->|scrape /actuator/prometheus| DS2
        PROM --> GRAF
    end

    DEV[Developer] -->|Basic Auth| DS1
    DEV -->|Basic Auth| DS2
```

### Luồng xác thực

```mermaid
sequenceDiagram
    participant Client as Service Client
    participant DS as Discovery Server
    participant SC as SecurityFilterChain

    Client->>DS: POST /eureka/apps/{appId}<br/>(Basic Auth in URL)
    DS->>SC: Filter request
    SC->>SC: Validate Basic Auth credentials
    alt Credentials hợp lệ
        SC->>DS: Allow request
        DS->>Client: 200 OK (registered)
    else Credentials không hợp lệ
        SC->>Client: 401 Unauthorized
    end

    Note over DS: Actuator /health endpoint<br/>không yêu cầu xác thực<br/>(cho Docker healthcheck)
```

### Luồng Peer Replication

```mermaid
sequenceDiagram
    participant S as Service Client
    participant DS1 as discovery-server-1
    participant DS2 as discovery-server-2

    S->>DS1: Register (Basic Auth)
    DS1->>DS1: Store in local registry
    DS1->>DS2: Replicate registration (peer sync)
    DS2->>DS2: Store in local registry

    Note over DS1,DS2: Nếu DS1 down, DS2 vẫn<br/>phục vụ registry từ bản sao local
    
    DS1--xDS2: DS1 goes down
    S->>DS2: Heartbeat / Discover (failover)
    DS2->>S: Return registry data

    Note over DS1,DS2: Khi DS1 khôi phục,<br/>tự động đồng bộ lại
    DS1->>DS2: Reconnect & sync registry
```

## Components and Interfaces

### 1. SecurityConfig (Tạo mới)

**Package**: `com.fintech.discovery.config`

**Mục đích**: Cấu hình Spring Security cho Discovery Server với Basic Auth.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/eureka/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(
            @Value("${app.security.username}") String username,
            @Value("${app.security.password}") String password) {
        UserDetails user = User.builder()
            .username(username)
            .password("{noop}" + password)
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
```

**Quyết định thiết kế**:
- **CSRF disable chỉ cho `/eureka/**`**: Eureka clients gửi POST/PUT/DELETE cho registration/heartbeat. Disable CSRF toàn bộ sẽ giảm bảo mật không cần thiết, nên chỉ disable cho Eureka API paths.
- **`/actuator/health` permitAll**: Docker healthcheck và load balancer cần truy cập health endpoint mà không cần credentials.
- **InMemoryUserDetailsManager với `{noop}`**: Discovery Server chỉ cần 1 user duy nhất cho Basic Auth. Không cần database hay BCrypt cho use case này. `{noop}` prefix cho biết password không mã hóa — phù hợp vì credentials được quản lý qua environment variables.
- **Tách biệt actuator endpoints**: `/actuator/health` public, các actuator khác yêu cầu auth.

### 2. Application Configuration Files

#### 2.1 application.yml (Cấu hình chung)

Chứa các thiết lập áp dụng cho tất cả môi trường:

```yaml
server:
  port: 8761
  shutdown: graceful

spring:
  application:
    name: discovery-server
  profiles:
    default: dev

app:
  security:
    username: ${EUREKA_USERNAME:eureka}
    password: ${EUREKA_PASSWORD:password}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: discovery-server

logging:
  level:
    org.springframework.web: WARN
    com.netflix: WARN
```

#### 2.2 application-dev.yml (Development - Standalone)

```yaml
eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${app.security.username}:${app.security.password}@localhost:${server.port}/eureka/
```

#### 2.3 application-docker.yml (Docker Compose - HA)

```yaml
eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:discovery-server}
    prefer-ip-address: false
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: ${EUREKA_PEER_URLS}
  server:
    enable-self-preservation: true
    renewal-percent-threshold: 0.49
```

**Quyết định thiết kế**:
- **`EUREKA_INSTANCE_HOSTNAME` từ env var**: Mỗi instance trong docker-compose có hostname khác nhau (discovery-server-1, discovery-server-2), truyền qua environment variable.
- **`EUREKA_PEER_URLS` từ env var**: URL của peer instance(s), cho phép cấu hình linh hoạt số lượng peers.
- **`prefer-ip-address: false`**: Trong Docker network, hostname resolution đáng tin cậy hơn IP (IP có thể thay đổi khi container restart).
- **`enable-self-preservation: true`**: Ngăn Eureka xóa instances khi có network partition tạm thời — quan trọng cho production.
- **`renewal-percent-threshold: 0.49`**: Giảm từ default 0.85 xuống 0.49 cho cluster nhỏ (2 instances) để tránh self-preservation mode kích hoạt quá sớm.

#### 2.4 application-prod.yml (Production - HA)

```yaml
eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME}
    prefer-ip-address: false
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: ${EUREKA_PEER_URLS}
  server:
    enable-self-preservation: true
    renewal-percent-threshold: 0.85
    eviction-interval-timer-in-ms: 30000
```

### 3. Structured Logging (logback-spring.xml)

Tái sử dụng pattern từ `account-service/src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="appName"
                    source="spring.application.name"
                    defaultValue="discovery-server"/>

    <!-- Dev profile: human-readable console output -->
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{correlationId:-}] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- Non-dev profiles: structured JSON output -->
    <springProfile name="!dev">
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>correlationId</includeMdcKeyName>
                <fieldNames>
                    <timestamp>timestamp</timestamp>
                    <version>[ignore]</version>
                </fieldNames>
                <timeZone>UTC</timeZone>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE"/>
        </root>
    </springProfile>

    <!-- Package-level log levels -->
    <logger name="com.fintech" level="INFO"/>
    <logger name="org.springframework.web" level="WARN"/>
    <logger name="com.netflix" level="WARN"/>
</configuration>
```

**Quyết định thiết kế**: Copy gần như nguyên bản từ account-service, chỉ thay đổi package-level loggers phù hợp (thêm `com.netflix` thay vì `org.hibernate.SQL` và `org.apache.kafka`).

### 4. Dockerfile (Cập nhật)

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY common/src common/src
COPY discovery-server/src discovery-server/src
RUN apk add --no-cache maven && \
    mvn -pl common,discovery-server -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/discovery-server/target/*.jar app.jar
EXPOSE 8761
ENV SPRING_PROFILES_ACTIVE=docker
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Quyết định thiết kế**:
- **Thêm `curl`**: Alpine JRE image không có curl mặc định, cần cho HEALTHCHECK.
- **`SPRING_PROFILES_ACTIVE=docker`**: Default profile trong container là `docker`, có thể override bằng environment variable.
- **HEALTHCHECK sử dụng `/actuator/health`**: Endpoint này được permitAll trong SecurityConfig, không cần credentials.
- **`start-period=40s`**: Eureka Server cần thời gian khởi động, đặc biệt khi chờ peer connection.

### 5. Docker Compose Configuration (Cập nhật)

Thay thế 1 discovery-server bằng 2 instances:

```yaml
discovery-server-1:
  build:
    context: .
    dockerfile: discovery-server/Dockerfile
  ports:
    - "8761:8761"
  environment:
    SPRING_PROFILES_ACTIVE: docker
    EUREKA_INSTANCE_HOSTNAME: discovery-server-1
    EUREKA_USERNAME: eureka
    EUREKA_PASSWORD: password
    EUREKA_PEER_URLS: http://eureka:password@discovery-server-2:8761/eureka/
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
    interval: 30s
    timeout: 10s
    start-period: 40s
    retries: 3

discovery-server-2:
  build:
    context: .
    dockerfile: discovery-server/Dockerfile
  ports:
    - "8762:8761"
  environment:
    SPRING_PROFILES_ACTIVE: docker
    EUREKA_INSTANCE_HOSTNAME: discovery-server-2
    EUREKA_USERNAME: eureka
    EUREKA_PASSWORD: password
    EUREKA_PEER_URLS: http://eureka:password@discovery-server-1:8761/eureka/
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
    interval: 30s
    timeout: 10s
    start-period: 40s
    retries: 3
```

Service clients sẽ cập nhật `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` trỏ đến cả 2 instances:
```
http://eureka:password@discovery-server-1:8761/eureka/,http://eureka:password@discovery-server-2:8761/eureka/
```

### 6. Prometheus Configuration (Cập nhật)

Thêm scrape config cho 2 discovery server instances:

```yaml
- job_name: 'discovery-server'
  metrics_path: /actuator/prometheus
  basic_auth:
    username: eureka
    password: password
  static_configs:
    - targets: ['discovery-server-1:8761', 'discovery-server-2:8761']
```

**Quyết định thiết kế**: Prometheus cần Basic Auth credentials vì `/actuator/prometheus` yêu cầu xác thực (chỉ `/actuator/health` là public).

### 7. Integration Tests

**Package**: `com.fintech.discovery`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class DiscoveryServerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Verifies Eureka Server starts successfully
    }

    @Test
    void healthEndpointReturnsUp() {
        // GET /actuator/health -> 200, status: UP
    }

    @Test
    void dashboardRequiresAuth() {
        // GET / without auth -> 401
    }

    @Test
    void dashboardAccessibleWithAuth() {
        // GET / with Basic Auth -> 200
    }

    @Test
    void eurekaApiRequiresAuth() {
        // GET /eureka/apps without auth -> 401
    }

    @Test
    void eurekaApiAccessibleWithAuth() {
        // GET /eureka/apps with Basic Auth -> 200
    }
}
```

**Quyết định thiết kế**:
- **`@ActiveProfiles("dev")`**: Test chạy ở standalone mode, không cần peer connection.
- **`TestRestTemplate`**: Spring Boot cung cấp sẵn, hỗ trợ Basic Auth dễ dàng.
- **Không mock Eureka Server**: Integration test cần verify server thực sự khởi động và hoạt động.

## Data Models

Feature này không tạo data model mới. Discovery Server sử dụng Eureka's internal registry (in-memory) để lưu trữ service registrations. Các cấu hình được quản lý qua Spring configuration properties:

### Configuration Properties

| Property | Type | Mô tả |
|---|---|---|
| `app.security.username` | String | Username cho Basic Auth (từ env var `EUREKA_USERNAME`) |
| `app.security.password` | String | Password cho Basic Auth (từ env var `EUREKA_PASSWORD`) |
| `eureka.instance.hostname` | String | Hostname của instance (từ env var `EUREKA_INSTANCE_HOSTNAME`) |
| `eureka.client.register-with-eureka` | boolean | Có đăng ký với peer không (false cho dev, true cho docker/prod) |
| `eureka.client.fetch-registry` | boolean | Có fetch registry từ peer không (false cho dev, true cho docker/prod) |
| `eureka.client.service-url.defaultZone` | String | URL(s) của Eureka peer(s) (từ env var `EUREKA_PEER_URLS`) |

### Environment Variables

| Variable | Default | Mô tả |
|---|---|---|
| `EUREKA_USERNAME` | `eureka` | Username cho Basic Auth |
| `EUREKA_PASSWORD` | `password` | Password cho Basic Auth |
| `EUREKA_INSTANCE_HOSTNAME` | `discovery-server` | Hostname của instance trong Docker |
| `EUREKA_PEER_URLS` | — | URL(s) peer instance(s) với credentials |
| `SPRING_PROFILES_ACTIVE` | `dev` (app) / `docker` (Dockerfile) | Active Spring profile |

## Error Handling

### 1. Xác thực thất bại (Authentication Failure)

| Tình huống | Hành vi | HTTP Status |
|---|---|---|
| Request không có credentials đến Dashboard/API | Trả về 401, yêu cầu Basic Auth | 401 Unauthorized |
| Request có credentials sai đến Dashboard/API | Trả về 401 | 401 Unauthorized |
| Request không có credentials đến `/actuator/health` | Cho phép truy cập (permitAll) | 200 OK |
| Request có credentials hợp lệ | Cho phép truy cập | 200 OK |

Spring Security xử lý tự động qua `httpBasic(Customizer.withDefaults())` — không cần custom error handler.

### 2. Peer Connection Failure

| Tình huống | Hành vi |
|---|---|
| Peer instance không khả dụng khi khởi động | Eureka Server khởi động bình thường, log WARN về peer unavailable, tiếp tục retry |
| Peer instance down trong quá trình hoạt động | Eureka tiếp tục phục vụ từ local registry, log WARN, tự động retry kết nối |
| Peer instance khôi phục | Eureka tự động đồng bộ registry, log INFO về successful sync |
| Tất cả peers down | Mỗi instance hoạt động độc lập với local registry |

Eureka Server có built-in resilience cho peer failures — không cần custom error handling. Self-preservation mode ngăn việc xóa instances khi có network issues.

### 3. Service Client Registration Failure

| Tình huống | Hành vi |
|---|---|
| Client gửi credentials sai | 401 Unauthorized, client retry với exponential backoff (Eureka client default) |
| Discovery Server không khả dụng | Client retry kết nối, sử dụng cached registry nếu có |
| Client gửi request malformed | Eureka trả về 400 Bad Request |

### 4. Health Check Failure

| Tình huống | Hành vi |
|---|---|
| `/actuator/health` trả về DOWN | Docker healthcheck đánh dấu container unhealthy sau 3 retries |
| Eureka Server OOM hoặc crash | Container restart theo Docker restart policy |
| Health endpoint timeout | Docker healthcheck retry sau interval (30s) |

## Testing Strategy

### Đánh giá Property-Based Testing

Feature này **không phù hợp** cho Property-Based Testing vì:

1. **Infrastructure as Code**: Phần lớn thay đổi là cấu hình Docker, docker-compose, YAML profiles — đây là declarative configuration, không phải functions với inputs/outputs.
2. **Security wiring**: Spring Security configuration là wiring/setup, không có business logic biến đổi theo input.
3. **Configuration validation**: Kiểm tra YAML files có đúng format và giá trị — phù hợp với example-based tests hơn.
4. **Side-effect operations**: Eureka peer replication, health checks, Prometheus scraping — đều là side effects, không có return value để assert universal properties.
5. **Integration verification**: Mục tiêu chính là verify các components hoạt động đúng khi kết hợp với nhau — phù hợp với integration tests.

Do đó, **Correctness Properties section được bỏ qua** và testing strategy tập trung vào integration tests và example-based unit tests.

### Integration Tests (Trọng tâm)

**Framework**: Spring Boot Test + JUnit 5 + TestRestTemplate

**File**: `discovery-server/src/test/java/com/fintech/discovery/DiscoveryServerApplicationTests.java`

| Test | Mô tả | Validates |
|---|---|---|
| `contextLoads()` | Verify Eureka Server khởi động thành công trong test context | Req 2.4, 3.1 |
| `healthEndpointReturnsUp()` | GET `/actuator/health` → 200, status UP, không cần auth | Req 3.2, 7.4, 7.5 |
| `dashboardRequiresAuth()` | GET `/` không có auth → 401 | Req 1.1 |
| `dashboardAccessibleWithAuth()` | GET `/` với Basic Auth → 200 | Req 1.3 |
| `eurekaApiRequiresAuth()` | GET `/eureka/apps` không có auth → 401 | Req 1.2 |
| `eurekaApiAccessibleWithAuth()` | GET `/eureka/apps` với Basic Auth → 200 | Req 1.4 |

**Cấu hình test**:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` — khởi động full server
- `@ActiveProfiles("dev")` — standalone mode, không cần peer
- Sử dụng `TestRestTemplate` cho HTTP requests với Basic Auth support

### Manual / Docker Compose Verification

Các yêu cầu sau cần verify thủ công trong môi trường docker-compose:

| Verification | Mô tả | Validates |
|---|---|---|
| HA Peer Replication | Khởi động 2 instances, verify registry sync | Req 6.1, 6.2, 6.3 |
| Peer Failure Recovery | Stop 1 instance, verify còn lại hoạt động, restart và verify sync | Req 6.4, 6.5 |
| Service Registration | Khởi động service client, verify đăng ký thành công trên cả 2 instances | Req 1.6, 8.5 |
| Prometheus Scraping | Verify Prometheus thu thập metrics từ cả 2 instances | Req 7.3, 7.6 |
| Docker Healthcheck | Verify container health status qua `docker ps` | Req 7.5, 9.2 |
| Profile Activation | Verify `SPRING_PROFILES_ACTIVE=docker` kích hoạt đúng config | Req 9.1, 9.3 |
| Structured Logging | Verify JSON log output trong docker-compose | Req 5.2, 5.3 |

### Build Verification

| Verification | Mô tả | Validates |
|---|---|---|
| Maven Build | `mvn clean package` thành công không lỗi | Req 2.3 |
| Docker Build | `docker build` thành công | Req 9.1 |
| Docker Compose Up | `docker-compose up` khởi động tất cả services | Req 8.1 |
