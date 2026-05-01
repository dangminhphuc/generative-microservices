# Requirements Document — Fintech Microservices Platform

## Intent Analysis

| Attribute | Value |
|---|---|
| **User Request** | Build dự án theo microservice pattern + DDD + Java Spring Boot + Clean Architecture |
| **Request Type** | New Project (Greenfield) |
| **Scope Estimate** | System-wide (multiple microservices) |
| **Complexity Estimate** | Complex |
| **Domain** | Fintech / Banking |

---

## 1. Functional Requirements

### FR-01: Microservices Architecture
- Hệ thống được xây dựng theo kiến trúc microservices với 2-3 services ban đầu
- Mỗi service là một bounded context riêng biệt, được xác định thông qua phân tích DDD
- AI-DLC sẽ đề xuất các bounded context phù hợp cho domain Fintech/Banking
- Các service giao tiếp qua hai kênh:
  - **Synchronous (REST API)**: cho các query operations
  - **Asynchronous (Apache Kafka)**: cho các command/state change operations

### FR-02: Domain-Driven Design (Tactical DDD)
- Áp dụng đầy đủ Tactical DDD patterns:
  - **Entity**: Đối tượng có identity duy nhất, có lifecycle
  - **Value Object**: Đối tượng bất biến, so sánh bằng giá trị
  - **Aggregate**: Đơn vị nhất quán (consistency boundary), chỉ Aggregate Root mới có Repository
  - **Domain Event**: Sự kiện domain được publish qua Kafka cho inter-service communication
  - **Repository**: Interface trong domain layer, implementation trong infrastructure layer
  - **Domain Service**: Logic nghiệp vụ liên quan đến nhiều aggregate

### FR-03: Hexagonal Architecture (Ports & Adapters)
- Mỗi service áp dụng Hexagonal Architecture:
  - **Domain Layer** (core): Entities, Value Objects, Aggregates, Domain Events, Domain Services, Repository Ports (interfaces)
  - **Application Layer**: Use Cases / Application Services, Input Ports, Output Ports
  - **Infrastructure Layer (Adapters)**: 
    - **Inbound Adapters**: REST Controllers, Kafka Consumers
    - **Outbound Adapters**: JPA Repository implementations, Kafka Producers, External API clients
- Dependency rule: Domain layer không phụ thuộc vào bất kỳ layer nào khác

### FR-04: API Gateway
- Spring Cloud Gateway làm single entry point cho tất cả client requests
- Routing requests đến các downstream services
- Cross-cutting concerns: authentication, rate limiting, logging

### FR-05: Service Discovery
- Spring Cloud Netflix Eureka cho service registration và discovery
- Mỗi service tự đăng ký với Eureka Server khi khởi động
- API Gateway sử dụng Eureka để resolve service locations

### FR-06: Authentication & Authorization
- Spring Security + JWT cho authentication
- JWT token validation tại API Gateway level
- Role-based access control (RBAC) cho authorization
- Stateless authentication — không dùng server-side session

---

## 2. Non-Functional Requirements

### NFR-01: Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.x (latest stable) |
| Build Tool | Maven | 3.9+ |
| Database | PostgreSQL | 16+ |
| Message Broker | Apache Kafka | 3.x |
| API Gateway | Spring Cloud Gateway | latest compatible |
| Service Discovery | Spring Cloud Netflix Eureka | latest compatible |
| Auth | Spring Security + JWT | latest compatible |
| Containerization | Docker + Docker Compose | latest |

### NFR-02: Database Strategy
- Database per Service pattern — mỗi service có PostgreSQL database riêng
- Không chia sẻ database giữa các services
- Mỗi service quản lý schema migration riêng (Flyway hoặc Liquibase)

### NFR-03: Observability
- Spring Boot Actuator cho health checks và metrics endpoints
- Micrometer cho metrics collection
- Prometheus cho metrics storage
- Grafana cho dashboards và visualization
- Structured logging với SLF4J/Logback
- Correlation ID propagation giữa các services

### NFR-04: Deployment
- Docker Compose cho development và staging environment
- Mỗi service có Dockerfile riêng
- Docker Compose file orchestrate tất cả services + infrastructure (PostgreSQL, Kafka, Eureka, Prometheus, Grafana)

### NFR-05: Project Structure
- Mono-repo với multi-module Maven project
- Mỗi service là một Maven module riêng
- Shared libraries (common DTOs, events, utilities) là các module riêng

### NFR-06: Testing Strategy
- Unit tests cho domain logic
- Integration tests cho adapters (repository, API, Kafka)
- Property-Based Testing (PBT) với jqwik framework cho:
  - Round-trip properties (serialization/deserialization)
  - Invariant properties (business rules)
  - Idempotency properties
  - Domain object generators
- Example-based tests bổ sung cho critical business paths

### NFR-07: Security Baseline
- Encryption in transit (TLS) cho tất cả communications
- Input validation trên tất cả API endpoints
- Parameterized queries — không string concatenation cho database operations
- Structured logging — không log sensitive data (passwords, tokens, PII)
- HTTP Security Headers cho web-facing endpoints
- Dependency vulnerability scanning
- Secure error handling — không expose stack traces trong production

---

## 3. Architectural Decisions

### AD-01: Hexagonal Architecture over Layered Architecture
- **Decision**: Sử dụng Hexagonal Architecture (Ports & Adapters) thay vì traditional layered architecture
- **Rationale**: Tách biệt hoàn toàn domain logic khỏi infrastructure concerns, dễ test, dễ thay đổi adapter mà không ảnh hưởng domain

### AD-02: CQRS-lite with Kafka
- **Decision**: Sử dụng REST cho queries, Kafka events cho commands/state changes
- **Rationale**: Phù hợp với Fintech domain — cần eventual consistency cho transactions, strong consistency cho reads

### AD-03: Database per Service
- **Decision**: Mỗi service có database riêng
- **Rationale**: Loose coupling, independent deployability, phù hợp microservices best practices

### AD-04: Mono-repo Multi-module
- **Decision**: Tất cả services trong một Maven multi-module project
- **Rationale**: Dễ quản lý dependencies, shared libraries, và cross-service refactoring cho team nhỏ

---

## 4. Constraints

- **C-01**: Java 21 LTS — sử dụng virtual threads, pattern matching, records khi phù hợp
- **C-02**: Maven build — consistent build across all modules
- **C-03**: Docker Compose — deployment target cho giai đoạn development
- **C-04**: Security Baseline extension — tất cả SECURITY rules là blocking constraints
- **C-05**: Property-Based Testing extension — tất cả PBT rules là blocking constraints
