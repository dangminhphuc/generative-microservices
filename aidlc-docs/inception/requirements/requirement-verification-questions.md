# Câu Hỏi Xác Minh Yêu Cầu

Vui lòng trả lời các câu hỏi sau bằng cách điền lựa chọn sau tag [Answer]:

---

## Phần 1: Business Domain & Scope

## Question 1
Dự án này thuộc lĩnh vực (domain) nào?

A) E-commerce (thương mại điện tử)
B) Fintech / Banking (tài chính, ngân hàng)
C) Healthcare (y tế, sức khỏe)
D) Logistics / Supply Chain (vận chuyển, chuỗi cung ứng)
E) SaaS Platform (nền tảng phần mềm dịch vụ)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]:  B

## Question 2
Bạn đã xác định được các bounded context / microservice nào chưa? Nếu có, hãy liệt kê.

A) Chưa xác định — cần AI-DLC hỗ trợ phân tích và đề xuất
B) Đã có ý tưởng sơ bộ (vui lòng mô tả sau tag [Answer]:)
C) Đã xác định rõ ràng (vui lòng liệt kê sau tag [Answer]:)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

## Question 3
Quy mô dự kiến của hệ thống (số lượng microservice)?

A) Nhỏ (2-3 services)
B) Trung bình (4-6 services)
C) Lớn (7-10 services)
D) Rất lớn (>10 services)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

---

## Phần 2: Technical Stack & Architecture

## Question 4
Phiên bản Java bạn muốn sử dụng?

A) Java 17 (LTS)
B) Java 21 (LTS — khuyến nghị)
C) Java 23+ (mới nhất)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: B

## Question 5
Build tool bạn muốn sử dụng?

A) Maven
B) Gradle (Groovy DSL)
C) Gradle (Kotlin DSL)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

## Question 6
Kiểu giao tiếp giữa các microservice?

A) Synchronous (REST API) là chính
B) Asynchronous (Event-driven với message broker) là chính
C) Kết hợp cả hai (REST cho query, Event cho command/state change)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: C

## Question 7
Message broker / Event streaming platform (nếu dùng async)?

A) Apache Kafka
B) RabbitMQ
C) AWS SQS/SNS
D) Chưa cần async communication ở giai đoạn này
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

## Question 8
Database strategy?

A) Mỗi service một database riêng (Database per Service — khuyến nghị cho microservices)
B) Shared database giữa một số service
C) Kết hợp — một số service dùng riêng, một số chia sẻ
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

## Question 9
Loại database chính?

A) PostgreSQL
B) MySQL
C) MongoDB (NoSQL)
D) Kết hợp nhiều loại (Polyglot Persistence)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

---

## Phần 3: Infrastructure & Deployment

## Question 10
Môi trường triển khai (deployment)?

A) Docker + Kubernetes
B) Docker Compose (cho development/staging)
C) AWS ECS / Fargate
D) Chỉ cần chạy local trước, deployment tính sau
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: B

## Question 11
API Gateway pattern?

A) Spring Cloud Gateway
B) Kong / Nginx
C) AWS API Gateway
D) Chưa cần API Gateway ở giai đoạn này
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

## Question 12
Service Discovery?

A) Spring Cloud Netflix Eureka
B) Kubernetes DNS (nếu dùng K8s)
C) Consul
D) Chưa cần ở giai đoạn này
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

---

## Phần 4: Cross-Cutting Concerns

## Question 13
Authentication & Authorization?

A) Spring Security + JWT
B) OAuth2 / OpenID Connect (Keycloak, Auth0)
C) AWS Cognito
D) Chưa cần ở giai đoạn đầu
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

## Question 14
Observability (Logging, Monitoring, Tracing)?

A) ELK Stack (Elasticsearch, Logstash, Kibana) + Zipkin/Jaeger
B) Spring Boot Actuator + Micrometer + Prometheus + Grafana
C) AWS CloudWatch
D) Chỉ cần logging cơ bản (SLF4J/Logback) ở giai đoạn đầu
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: B

---

## Phần 5: DDD & Clean Architecture Details

## Question 15
Mức độ áp dụng DDD bạn mong muốn?

A) Tactical DDD đầy đủ (Entity, Value Object, Aggregate, Domain Event, Repository, Domain Service)
B) Strategic DDD (Bounded Context, Context Map) + Tactical cơ bản
C) Cả Strategic và Tactical DDD đầy đủ
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

## Question 16
Clean Architecture layers bạn muốn áp dụng?

A) 4 layers: Domain → Application (Use Cases) → Infrastructure → Presentation (API)
B) Hexagonal Architecture (Ports & Adapters) — tương tự Clean Architecture
C) Kết hợp Clean Architecture + Hexagonal (Ports & Adapters cho infrastructure, Use Cases cho application)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: B

---

## Phần 6: Project Structure

## Question 17
Cấu trúc repository?

A) Mono-repo (tất cả services trong một repository)
B) Multi-repo (mỗi service một repository riêng)
C) Mono-repo nhưng mỗi service là một module riêng (multi-module project)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: C

---

## Phần 7: Extensions

## Question 18: Security Extension
Có nên áp dụng các quy tắc bảo mật (Security Baseline) cho dự án này không?

A) Có — áp dụng tất cả quy tắc SECURITY như ràng buộc bắt buộc (khuyến nghị cho ứng dụng production)
B) Không — bỏ qua quy tắc SECURITY (phù hợp cho PoC, prototype, dự án thử nghiệm)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

## Question 19: Property-Based Testing Extension
Có nên áp dụng Property-Based Testing (PBT) cho dự án này không?

A) Có — áp dụng tất cả quy tắc PBT như ràng buộc bắt buộc (khuyến nghị cho dự án có business logic, data transformation, serialization)
B) Một phần — chỉ áp dụng PBT cho pure functions và serialization round-trips
C) Không — bỏ qua quy tắc PBT (phù hợp cho ứng dụng CRUD đơn giản, UI-only, hoặc thin integration layer)
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A
