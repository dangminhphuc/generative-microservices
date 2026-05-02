# Implementation Plan: Discovery Server Production-Ready

## Overview

Triển khai từng bước để đưa Discovery Server (Eureka) lên mức production-ready. Mỗi task tạo một feature branch riêng theo Git Flow model (`feature/discovery-xxx` từ `develop`), thực hiện thay đổi, commit, và yêu cầu xác nhận merge về `develop` trước khi chuyển sang task tiếp theo. Thứ tự thực hiện theo dependency: nâng cấp nền tảng trước, sau đó thêm tính năng, cuối cùng là integration và documentation.

## Tasks

- [x] 1. Nâng cấp Spring Boot và Spring Cloud + Thêm Dependencies (Branch: `feature/discovery-spring-upgrade`)
  - [x] 1.1 Tạo branch `feature/discovery-spring-upgrade` từ `develop`, nâng cấp Spring Boot lên 3.5.x và Spring Cloud lên 2025.0.0 trong parent `pom.xml`
    - Cập nhật `<version>` của `spring-boot-starter-parent` từ `3.4.1` lên `3.5.14` (hoặc phiên bản stable mới nhất trong dòng 3.5)
    - Cập nhật property `<spring-cloud.version>` từ `2024.0.0` lên `2025.0.0`
    - Chạy `mvn clean compile -pl discovery-server -am` để verify build thành công
    - Commit changes với message: `chore: upgrade Spring Boot to 3.5.x and Spring Cloud to 2025.0.0`
    - _Requirements: 2.1, 2.2, 2.3_
  - [x] 1.2 Trên cùng branch `feature/discovery-spring-upgrade`, thêm các dependencies cần thiết vào `discovery-server/pom.xml`
    - Thêm `spring-boot-starter-security` dependency
    - Thêm `spring-boot-starter-actuator` dependency
    - Thêm `micrometer-registry-prometheus` dependency (runtime scope)
    - Thêm `logstash-logback-encoder` dependency (sử dụng version từ parent `${logstash-logback.version}`)
    - Thêm `spring-boot-starter-test` dependency (test scope)
    - Thêm `spring-security-test` dependency (test scope)
    - Chạy `mvn clean compile -pl discovery-server -am` để verify tất cả dependencies resolve thành công
    - Commit changes với message: `feat: add security, actuator, prometheus, logging, and test dependencies`
    - _Requirements: 1.1, 1.2, 7.1, 5.1, 3.1_
  - [x] 1.3 Checkpoint - Verify nền tảng: ensure build thành công với `mvn clean compile -pl discovery-server -am`
  - [x] 1.4 🔀 Yêu cầu xác nhận merge branch `feature/discovery-spring-upgrade` về `develop`

- [x] 2. Cấu hình Security - Basic Auth (Branch: `feature/discovery-security`)
  - [x] 2.1 Tạo branch `feature/discovery-security` từ `develop`, tạo file `SecurityConfig.java` trong package `com.fintech.discovery.config`
    - Tạo class `SecurityConfig` với annotation `@Configuration` và `@EnableWebSecurity`
    - Cấu hình `SecurityFilterChain` bean: CSRF disable cho `/eureka/**`, permitAll cho `/actuator/health` và `/actuator/health/**`, authenticated cho tất cả request còn lại, enable httpBasic
    - Cấu hình `InMemoryUserDetailsManager` bean đọc username/password từ `@Value("${app.security.username}")` và `@Value("${app.security.password}")`, sử dụng `{noop}` prefix cho password
    - Commit changes với message: `feat: add Basic Auth security configuration for Eureka`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.7, 7.4_
  - [x] 2.2 🔀 Yêu cầu xác nhận merge branch `feature/discovery-security` về `develop`

- [x] 3. Cấu hình Environment Profiles (Branch: `feature/discovery-profiles`)
  - [x] 3.1 Tạo branch `feature/discovery-profiles` từ `develop`, cập nhật `discovery-server/src/main/resources/application.yml` với cấu hình chung
    - Thêm `server.shutdown: graceful`
    - Thêm `spring.profiles.default: dev`
    - Thêm block `app.security.username` và `app.security.password` đọc từ env vars `EUREKA_USERNAME` và `EUREKA_PASSWORD` với default values
    - Thêm cấu hình `management.endpoints.web.exposure.include: health,info,metrics,prometheus`
    - Thêm `management.metrics.tags.application: discovery-server`
    - Thêm logging level WARN cho `org.springframework.web` và `com.netflix`
    - Xóa cấu hình eureka cũ (sẽ chuyển sang profile-specific files)
    - _Requirements: 4.1, 4.5, 1.5, 7.2, 5.5_
  - [x] 3.2 Tạo file `discovery-server/src/main/resources/application-dev.yml` cho môi trường development
    - Cấu hình `eureka.instance.hostname: localhost`
    - Cấu hình `eureka.client.register-with-eureka: false` và `eureka.client.fetch-registry: false` (standalone mode)
    - Cấu hình `eureka.client.service-url.defaultZone` với Basic Auth credentials trong URL
    - _Requirements: 4.2, 4.5_
  - [x] 3.3 Tạo file `discovery-server/src/main/resources/application-docker.yml` cho môi trường docker-compose HA
    - Cấu hình `eureka.instance.hostname` đọc từ env var `EUREKA_INSTANCE_HOSTNAME`
    - Cấu hình `eureka.instance.prefer-ip-address: false`
    - Cấu hình `eureka.client.register-with-eureka: true` và `eureka.client.fetch-registry: true`
    - Cấu hình `eureka.client.service-url.defaultZone` đọc từ env var `EUREKA_PEER_URLS`
    - Cấu hình `eureka.server.enable-self-preservation: true` và `eureka.server.renewal-percent-threshold: 0.49`
    - _Requirements: 4.3, 6.1, 6.2_
  - [x] 3.4 Tạo file `discovery-server/src/main/resources/application-prod.yml` cho môi trường production
    - Cấu hình tương tự docker profile nhưng với `renewal-percent-threshold: 0.85` và `eviction-interval-timer-in-ms: 30000`
    - _Requirements: 4.4, 6.1_
  - Commit tất cả changes với message: `feat: add multi-environment profile configuration (dev, docker, prod)`
  - [x] 3.5 🔀 Yêu cầu xác nhận merge branch `feature/discovery-profiles` về `develop`

- [x] 4. Structured Logging (Branch: `feature/discovery-logging`)
  - [x] 4.1 Tạo branch `feature/discovery-logging` từ `develop`, tạo file `discovery-server/src/main/resources/logback-spring.xml`
    - Cấu hình `springProperty` để đọc `spring.application.name`
    - Cấu hình profile `dev`: ConsoleAppender với pattern human-readable bao gồm timestamp, thread, correlationId, log level, logger, message
    - Cấu hình profile `!dev`: ConsoleAppender với `LogstashEncoder` cho JSON output, include `correlationId` từ MDC
    - Cấu hình package-level loggers: `com.fintech` INFO, `org.springframework.web` WARN, `com.netflix` WARN
    - Commit changes với message: `feat: add structured logging with JSON output for non-dev profiles`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  - [x] 4.2 Checkpoint - Verify cấu hình cơ bản: ensure build thành công và application khởi động được với profile dev bằng `mvn clean package -pl discovery-server -am -DskipTests`
  - [x] 4.3 🔀 Yêu cầu xác nhận merge branch `feature/discovery-logging` về `develop`

- [x] 5. Monitoring và Health Check - Prometheus config (Branch: `feature/discovery-monitoring`)
  - [x] 5.1 Tạo branch `feature/discovery-monitoring` từ `develop`, cập nhật `infra/prometheus/prometheus.yml` thêm scrape config cho discovery servers
    - Thêm job `discovery-server` với `metrics_path: /actuator/prometheus`
    - Cấu hình `basic_auth` với username và password cho Eureka
    - Cấu hình `static_configs.targets` trỏ đến `discovery-server-1:8761` và `discovery-server-2:8761`
    - Commit changes với message: `feat: add Prometheus scrape config for discovery server instances`
    - _Requirements: 7.3, 7.6_
  - [x] 5.2 🔀 Yêu cầu xác nhận merge branch `feature/discovery-monitoring` về `develop`

- [x] 6. Cập nhật Dockerfile (Branch: `feature/discovery-dockerfile`)
  - [x] 6.1 Tạo branch `feature/discovery-dockerfile` từ `develop`, cập nhật `discovery-server/Dockerfile`
    - Thêm `RUN apk add --no-cache curl` trong runtime stage (cần cho healthcheck)
    - Thêm `ENV SPRING_PROFILES_ACTIVE=docker` để set default profile trong container
    - Thêm `HEALTHCHECK` instruction: `--interval=30s --timeout=10s --start-period=40s --retries=3 CMD curl -f http://localhost:8761/actuator/health || exit 1`
    - Commit changes với message: `feat: add profile support and healthcheck to Dockerfile`
    - _Requirements: 9.1, 9.2, 9.3_
  - [x] 6.2 🔀 Yêu cầu xác nhận merge branch `feature/discovery-dockerfile` về `develop`

- [x] 7. Cập nhật Docker Compose cho High Availability (Branch: `feature/discovery-ha-compose`)
  - [x] 7.1 Tạo branch `feature/discovery-ha-compose` từ `develop`, cập nhật `docker-compose.yml` thay thế service `discovery-server` bằng 2 instances
    - Xóa service `discovery-server` hiện tại
    - Thêm service `discovery-server-1`: build từ `discovery-server/Dockerfile`, port `8761:8761`, environment variables (`SPRING_PROFILES_ACTIVE=docker`, `EUREKA_INSTANCE_HOSTNAME=discovery-server-1`, `EUREKA_USERNAME`, `EUREKA_PASSWORD`, `EUREKA_PEER_URLS` trỏ đến discovery-server-2), healthcheck config
    - Thêm service `discovery-server-2`: tương tự nhưng port `8762:8761`, hostname `discovery-server-2`, `EUREKA_PEER_URLS` trỏ đến discovery-server-1
    - Cập nhật tất cả service clients (`api-gateway`, `account-service`, `transfer-service`, `transaction-history-service`): đổi `depends_on` từ `discovery-server` sang `discovery-server-1` và `discovery-server-2`, cập nhật `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` trỏ đến cả 2 instances với Basic Auth credentials
    - Commit changes với message: `feat: configure HA discovery server cluster in docker-compose`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 6.3_
  - [x] 7.2 Checkpoint - Verify Docker build: ensure Dockerfile build thành công với `docker build -f discovery-server/Dockerfile .`
  - [x] 7.3 🔀 Yêu cầu xác nhận merge branch `feature/discovery-ha-compose` về `develop`

- [x] 8. Integration Tests (Branch: `feature/discovery-tests`)
  - [x] 8.1 Tạo branch `feature/discovery-tests` từ `develop`, tạo file test `discovery-server/src/test/java/com/fintech/discovery/DiscoveryServerApplicationTests.java`
    - Tạo test class với `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` và `@ActiveProfiles("dev")`
    - Inject `TestRestTemplate` để thực hiện HTTP requests
    - Implement test `contextLoads()`: verify Eureka Server khởi động thành công trong test context
    - Implement test `healthEndpointReturnsUp()`: GET `/actuator/health` → HTTP 200, body chứa `"status":"UP"`, không cần auth
    - Implement test `dashboardRequiresAuth()`: GET `/` không có auth → HTTP 401
    - Implement test `dashboardAccessibleWithAuth()`: GET `/` với Basic Auth credentials → HTTP 200
    - Implement test `eurekaApiRequiresAuth()`: GET `/eureka/apps` không có auth → HTTP 401
    - Implement test `eurekaApiAccessibleWithAuth()`: GET `/eureka/apps` với Basic Auth credentials → HTTP 200
    - Chạy `mvn test -pl discovery-server` để verify tất cả tests pass
    - Commit changes với message: `test: add integration tests for security, health, and Eureka API`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 1.1, 1.2, 1.3, 1.4, 7.4, 7.5_
  - [x] 8.2 Checkpoint - Verify tất cả tests pass: ensure all tests pass với `mvn clean verify -pl discovery-server -am`
  - [x] 8.3 🔀 Yêu cầu xác nhận merge branch `feature/discovery-tests` về `develop`

- [x] 9. Documentation (Branch: `feature/discovery-docs`)
  - [x] 9.1 Tạo branch `feature/discovery-docs` từ `develop`, tạo file `discovery-server/README.md`
    - Mô tả mục đích và vai trò của Discovery Server trong nền tảng fintech microservices
    - Liệt kê các Environment Profiles (dev, docker, prod) và mô tả sự khác biệt
    - Hướng dẫn chạy standalone (dev): `mvn spring-boot:run` hoặc IDE
    - Hướng dẫn chạy HA (docker): `docker-compose up discovery-server-1 discovery-server-2`
    - Mô tả cấu hình bảo mật Basic Auth và cách thay đổi credentials qua environment variables
    - Liệt kê Actuator endpoints có sẵn (`/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`) và cách truy cập
    - Mô tả cấu hình HA peer replication và hành vi khi peer failure
    - Commit changes với message: `docs: add comprehensive README for discovery server`
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_
  - [x] 9.2 🔀 Yêu cầu xác nhận merge branch `feature/discovery-docs` về `develop`

- [x] 10. Final Checkpoint - Verify toàn bộ
  - Ensure full build thành công với `mvn clean verify -pl discovery-server -am` và tất cả tests pass

## Notes

- Mỗi task group tạo feature branch riêng theo Git Flow model (`feature/discovery-xxx` từ `develop`), sẵn sàng cho merge về `develop`
- Sau khi hoàn thành mỗi task group, yêu cầu xác nhận merge branch trước khi chuyển sang task tiếp theo
- Thứ tự thực hiện theo dependency: Spring Boot upgrade + dependencies → security → profiles → logging → monitoring → Dockerfile → Docker Compose → tests → docs
- Task 1 gộp upgrade và dependencies cùng trên branch `feature/discovery-spring-upgrade` vì dependencies phụ thuộc vào version upgrade
- Checkpoints đảm bảo verify incremental tại các điểm quan trọng
- Tất cả credentials đọc từ environment variables, không hardcode
- Integration tests chạy với profile `dev` (standalone mode) để không cần peer connection
- Design document không có Correctness Properties section nên không sử dụng Property-Based Testing
