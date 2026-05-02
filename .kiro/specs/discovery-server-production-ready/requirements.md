# Requirements Document

## Giới thiệu

Tài liệu này mô tả các yêu cầu để đưa Discovery Server (Eureka) trong nền tảng fintech microservices lên mức production-ready. Hiện tại, Discovery Server chỉ có cấu hình tối thiểu: không có bảo mật, không có test, không có logging có cấu trúc, không có monitoring, không hỗ trợ High Availability, và chỉ có một file cấu hình duy nhất. Mục tiêu là xây dựng một hạ tầng gần giống production nhất có thể, sử dụng docker-compose cho môi trường phát triển local.

## Glossary

- **Discovery_Server**: Ứng dụng Spring Boot chạy Netflix Eureka Server, cung cấp service registry cho toàn bộ nền tảng fintech microservices.
- **Eureka_Dashboard**: Giao diện web của Eureka Server hiển thị danh sách các service đã đăng ký, trạng thái instance, và thông tin registry.
- **Eureka_API**: Các REST endpoint của Eureka Server dùng để đăng ký, hủy đăng ký, và truy vấn service registry (ví dụ: `/eureka/apps`).
- **Service_Client**: Bất kỳ microservice nào trong nền tảng (account-service, transfer-service, api-gateway, transaction-history-service) đăng ký và truy vấn registry thông qua Eureka client.
- **Actuator_Endpoint**: Các HTTP endpoint do Spring Boot Actuator cung cấp để giám sát sức khỏe, metrics, và thông tin ứng dụng.
- **Structured_Log**: Log output ở định dạng JSON có cấu trúc, bao gồm timestamp, log level, logger name, message, và correlationId, phù hợp cho việc thu thập và phân tích bằng các hệ thống log tập trung.
- **Peer_Instance**: Một instance Eureka Server khác trong cụm High Availability, đồng bộ registry với nhau.
- **Environment_Profile**: Cấu hình Spring profile (dev, docker, prod) xác định hành vi và tham số khác nhau cho từng môi trường triển khai.
- **Basic_Auth**: Cơ chế xác thực HTTP Basic Authentication sử dụng username và password.

## Requirements

### Requirement 1: Bảo mật Eureka Dashboard và API

**User Story:** Là một DevOps engineer, tôi muốn Eureka Dashboard và API được bảo vệ bằng xác thực, để chỉ những người và service được ủy quyền mới có thể xem và tương tác với service registry.

#### Acceptance Criteria

1. WHEN một request không có credentials hợp lệ truy cập Eureka_Dashboard, THE Discovery_Server SHALL trả về HTTP 401 Unauthorized và yêu cầu xác thực Basic_Auth.
2. WHEN một request không có credentials hợp lệ truy cập Eureka_API, THE Discovery_Server SHALL trả về HTTP 401 Unauthorized.
3. WHEN một request có credentials Basic_Auth hợp lệ truy cập Eureka_Dashboard, THE Discovery_Server SHALL cho phép truy cập và hiển thị dashboard.
4. WHEN một request có credentials Basic_Auth hợp lệ truy cập Eureka_API, THE Discovery_Server SHALL cho phép truy cập và trả về dữ liệu registry.
5. THE Discovery_Server SHALL đọc username và password cho Basic_Auth từ cấu hình bên ngoài (environment variables hoặc application properties), không hardcode trong source code.
6. WHEN một Service_Client đăng ký với Discovery_Server, THE Service_Client SHALL gửi credentials Basic_Auth trong URL kết nối Eureka (định dạng `http://user:password@host:port/eureka/`).
7. THE Discovery_Server SHALL tắt CSRF protection cho các Eureka_API endpoint để đảm bảo Service_Client có thể đăng ký và gửi heartbeat thành công.

### Requirement 2: Nâng cấp Spring Boot

**User Story:** Là một developer, tôi muốn nâng cấp Spring Boot lên phiên bản 3.5.x được hỗ trợ, để đảm bảo nhận được các bản vá bảo mật và bug fix mới nhất.

#### Acceptance Criteria

1. THE Discovery_Server SHALL sử dụng Spring Boot phiên bản 3.5.x (phiên bản stable mới nhất trong dòng 3.5).
2. THE Discovery_Server SHALL sử dụng phiên bản Spring Cloud tương thích với Spring Boot 3.5.x.
3. WHEN Discovery_Server được build sau khi nâng cấp, THE Discovery_Server SHALL compile và package thành công mà không có lỗi.
4. WHEN Discovery_Server khởi động sau khi nâng cấp, THE Discovery_Server SHALL khởi động thành công và sẵn sàng nhận đăng ký từ Service_Client.

### Requirement 3: Testing

**User Story:** Là một developer, tôi muốn có integration test cho Discovery Server, để đảm bảo server khởi động thành công và các chức năng cơ bản hoạt động đúng sau mỗi thay đổi.

#### Acceptance Criteria

1. WHEN integration test được chạy, THE Discovery_Server SHALL khởi động thành công trong test context với Eureka Server được kích hoạt.
2. WHEN integration test kiểm tra Actuator_Endpoint `/actuator/health`, THE Discovery_Server SHALL trả về HTTP 200 với status "UP".
3. WHEN integration test kiểm tra Eureka_Dashboard endpoint `/`, THE Discovery_Server SHALL trả về HTTP 200 khi có credentials hợp lệ.
4. WHEN integration test kiểm tra Eureka_API endpoint `/eureka/apps`, THE Discovery_Server SHALL trả về HTTP 200 với danh sách applications (có thể rỗng) khi có credentials hợp lệ.

### Requirement 4: Cấu hình theo môi trường (Environment Profiles)

**User Story:** Là một DevOps engineer, tôi muốn có cấu hình riêng cho từng môi trường (dev, docker, prod), để Discovery Server hoạt động đúng trong mỗi môi trường với hostname, peering, và tham số phù hợp.

#### Acceptance Criteria

1. THE Discovery_Server SHALL cung cấp file cấu hình `application.yml` chứa các thiết lập chung cho tất cả môi trường.
2. THE Discovery_Server SHALL cung cấp file cấu hình `application-dev.yml` cho môi trường phát triển local với `eureka.instance.hostname` là `localhost` và chế độ standalone (không peering).
3. THE Discovery_Server SHALL cung cấp file cấu hình `application-docker.yml` cho môi trường docker-compose với `eureka.instance.hostname` phù hợp với tên container và hỗ trợ peering giữa nhiều instance.
4. THE Discovery_Server SHALL cung cấp file cấu hình `application-prod.yml` cho môi trường production với cấu hình High Availability bắt buộc peering giữa ít nhất 2 Peer_Instance.
5. WHEN không có profile nào được chỉ định, THE Discovery_Server SHALL sử dụng profile `dev` làm mặc định.

### Requirement 5: Structured Logging

**User Story:** Là một DevOps engineer, tôi muốn Discovery Server có logging có cấu trúc (JSON), để log có thể được thu thập và phân tích bởi các hệ thống log tập trung (ELK, CloudWatch).

#### Acceptance Criteria

1. THE Discovery_Server SHALL sử dụng file cấu hình `logback-spring.xml` để định nghĩa logging configuration.
2. WHILE Environment_Profile `dev` đang hoạt động, THE Discovery_Server SHALL ghi log ra console ở định dạng human-readable với pattern bao gồm timestamp, thread, correlationId, log level, logger name, và message.
3. WHILE Environment_Profile khác `dev` đang hoạt động, THE Discovery_Server SHALL ghi Structured_Log ở định dạng JSON sử dụng LogstashEncoder.
4. THE Discovery_Server SHALL bao gồm `correlationId` từ MDC trong mỗi dòng log khi có sẵn.
5. THE Discovery_Server SHALL thiết lập log level `WARN` cho các package `org.springframework.web` và `com.netflix` để giảm log noise trong quá trình vận hành bình thường.

### Requirement 6: High Availability

**User Story:** Là một DevOps engineer, tôi muốn Discovery Server hỗ trợ chạy nhiều instance với peer replication, để service registry vẫn khả dụng khi một instance gặp sự cố.

#### Acceptance Criteria

1. WHILE Environment_Profile `docker` hoặc `prod` đang hoạt động, THE Discovery_Server SHALL cấu hình `eureka.client.register-with-eureka` là `true` và `eureka.client.fetch-registry` là `true` để cho phép peering.
2. WHILE Environment_Profile `docker` đang hoạt động, THE Discovery_Server SHALL cấu hình `eureka.client.service-url.defaultZone` trỏ đến các Peer_Instance khác trong cụm.
3. THE Discovery_Server SHALL hỗ trợ chạy ít nhất 2 instance trong docker-compose với peer replication hoạt động.
4. WHEN một Peer_Instance không khả dụng, THE Discovery_Server SHALL tiếp tục phục vụ service registry từ bản sao local và ghi log cảnh báo về peer không khả dụng.
5. WHEN một Peer_Instance khôi phục hoạt động, THE Discovery_Server SHALL tự động đồng bộ lại registry với Peer_Instance đó.

### Requirement 7: Monitoring và Health Check

**User Story:** Là một DevOps engineer, tôi muốn Discovery Server expose các endpoint health check và metrics, để hệ thống monitoring (Prometheus, Grafana) có thể giám sát trạng thái và hiệu suất của server.

#### Acceptance Criteria

1. THE Discovery_Server SHALL bao gồm dependency `spring-boot-starter-actuator` trong pom.xml.
2. THE Discovery_Server SHALL expose các Actuator_Endpoint: `health`, `info`, `metrics`, và `prometheus`.
3. WHEN Prometheus scrape endpoint `/actuator/prometheus`, THE Discovery_Server SHALL trả về metrics ở định dạng Prometheus text format.
4. THE Discovery_Server SHALL expose Actuator_Endpoint trên một port riêng biệt (management port) hoặc cho phép truy cập không cần xác thực đến `/actuator/health` để hỗ trợ container health check.
5. WHEN docker-compose health check gọi endpoint health, THE Discovery_Server SHALL trả về HTTP 200 khi server đang hoạt động bình thường.
6. THE Discovery_Server SHALL được cấu hình trong Prometheus scrape config (`infra/prometheus/prometheus.yml`) để metrics được thu thập tự động.

### Requirement 8: Cập nhật Docker Compose cho High Availability

**User Story:** Là một developer, tôi muốn docker-compose được cập nhật để chạy Discovery Server ở chế độ High Availability với 2 instance, để môi trường local phát triển gần giống production nhất có thể.

#### Acceptance Criteria

1. THE docker-compose.yml SHALL định nghĩa 2 Discovery_Server instance (discovery-server-1 và discovery-server-2) thay vì 1 instance duy nhất.
2. THE docker-compose.yml SHALL cấu hình mỗi Discovery_Server instance với environment variables chứa credentials Basic_Auth cho Eureka.
3. THE docker-compose.yml SHALL cấu hình mỗi Discovery_Server instance trỏ `eureka.client.service-url.defaultZone` đến Peer_Instance còn lại.
4. THE docker-compose.yml SHALL cấu hình health check cho mỗi Discovery_Server instance sử dụng Actuator_Endpoint health.
5. WHEN các Service_Client khởi động trong docker-compose, THE Service_Client SHALL cấu hình `eureka.client.service-url.defaultZone` trỏ đến cả 2 Discovery_Server instance.
6. THE docker-compose.yml SHALL expose port 8761 cho discovery-server-1 và port 8762 cho discovery-server-2 để truy cập từ host machine.

### Requirement 9: Cập nhật Dockerfile

**User Story:** Là một DevOps engineer, tôi muốn Dockerfile của Discovery Server hỗ trợ Spring profile và health check, để container hoạt động đúng trong môi trường docker-compose và production.

#### Acceptance Criteria

1. THE Dockerfile SHALL cho phép truyền Spring profile thông qua environment variable `SPRING_PROFILES_ACTIVE`.
2. THE Dockerfile SHALL định nghĩa `HEALTHCHECK` instruction sử dụng `curl` hoặc tương đương để kiểm tra Actuator_Endpoint health.
3. WHEN container khởi động với `SPRING_PROFILES_ACTIVE=docker`, THE Discovery_Server SHALL sử dụng cấu hình từ `application-docker.yml`.

### Requirement 10: Documentation

**User Story:** Là một developer mới tham gia dự án, tôi muốn có README cho Discovery Server, để tôi có thể hiểu cách cấu hình, chạy, và vận hành server trong các môi trường khác nhau.

#### Acceptance Criteria

1. THE Discovery_Server SHALL có file `README.md` trong thư mục `discovery-server/`.
2. THE README.md SHALL mô tả mục đích và vai trò của Discovery_Server trong nền tảng microservices.
3. THE README.md SHALL liệt kê các Environment_Profile có sẵn (dev, docker, prod) và mô tả sự khác biệt giữa chúng.
4. THE README.md SHALL hướng dẫn cách chạy Discovery_Server ở chế độ standalone (dev) và High Availability (docker/prod).
5. THE README.md SHALL mô tả cấu hình bảo mật (Basic_Auth) và cách thay đổi credentials.
6. THE README.md SHALL liệt kê các Actuator_Endpoint có sẵn và cách truy cập chúng.
