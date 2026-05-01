# Tech Stack Decisions — Account Service

## Runtime & Framework

| Component | Choice | Version | Rationale |
|---|---|---|---|
| Language | Java | 21 LTS | Virtual threads, records, pattern matching, sealed classes |
| Framework | Spring Boot | 3.4.x | Production-ready, extensive ecosystem |
| Build | Maven | 3.9+ | Team preference, multi-module support |

## Data & Persistence

| Component | Choice | Rationale |
|---|---|---|
| Database | PostgreSQL 16 | ACID compliance, JSON support, mature ecosystem |
| ORM | Spring Data JPA (Hibernate) | Standard JPA, repository pattern, query derivation |
| Migration | Flyway | SQL-based migrations, version control friendly |
| Connection Pool | HikariCP | Spring Boot default, high performance |

## Security

| Component | Choice | Rationale |
|---|---|---|
| Authentication | Spring Security + JWT | Stateless, microservice-friendly |
| JWT Library | jjwt (io.jsonwebtoken) | Mature, well-maintained, fluent API |
| Password Hashing | BCrypt (Spring Security) | Adaptive hashing, industry standard |
| Input Validation | Jakarta Bean Validation | Annotation-based, declarative |

## Messaging

| Component | Choice | Rationale |
|---|---|---|
| Message Broker | Apache Kafka | Event streaming, durability, replay capability |
| Kafka Client | Spring Kafka | Spring integration, auto-configuration |
| Serialization | JSON (Jackson) | Human-readable, debugging friendly |

## Testing

| Component | Choice | Rationale |
|---|---|---|
| Unit Testing | JUnit 5 | Standard Java testing framework |
| PBT Framework | jqwik 1.9.x | JUnit 5 integration, stateful testing, excellent shrinking (PBT-09) |
| Mocking | Mockito | Spring Boot default, widely adopted |
| Integration Testing | Spring Boot Test + Testcontainers | Real PostgreSQL + Kafka in tests |
| API Testing | MockMvc | Spring MVC test support |

## Caching

| Component | Choice | Rationale |
|---|---|---|
| Cache Abstraction | Spring Cache (`@Cacheable`, `@CacheEvict`) | Declarative, annotation-based, framework-native |
| Cache Provider | Caffeine | High-performance local cache, W-TinyLFU eviction, low latency |
| Cache Metrics | Micrometer Caffeine integration | Automatic hit/miss/eviction metrics via Actuator |

## Observability

| Component | Choice | Rationale |
|---|---|---|
| Logging | SLF4J + Logback | Spring Boot default, structured logging |
| Metrics | Micrometer + Prometheus | Spring Boot Actuator integration |
| Health Checks | Spring Boot Actuator | Built-in health indicators |

## Service Discovery

| Component | Choice | Rationale |
|---|---|---|
| Discovery | Spring Cloud Netflix Eureka Client | Service registration, load balancing |
| Client Load Balancing | Spring Cloud LoadBalancer | Eureka-aware, client-side LB |

## Dependencies Summary (pom.xml)

```xml
<!-- Core -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-security
spring-boot-starter-actuator

<!-- Cloud -->
spring-cloud-starter-netflix-eureka-client

<!-- Messaging -->
spring-kafka

<!-- Database -->
postgresql (driver)
flyway-core
flyway-database-postgresql

<!-- JWT -->
jjwt-api, jjwt-impl, jjwt-jackson

<!-- Caching -->
caffeine (com.github.ben-manes.caffeine:caffeine)

<!-- Metrics -->
micrometer-registry-prometheus

<!-- Common -->
com.fintech:common

<!-- Test -->
spring-boot-starter-test
spring-security-test
jqwik
testcontainers (postgresql, kafka)
```
