# Infrastructure Design — Account Service

## Infrastructure Mapping

| Logical Component | Infrastructure Service | Configuration |
|---|---|---|
| Application Runtime | Docker container (Eclipse Temurin 21 JRE Alpine) | Port 8081, 512MB heap |
| Database | PostgreSQL 16 (Docker: `account-db`) | Port 5432, `account_db` database |
| Message Broker | Apache Kafka (Docker: `kafka`) | Port 9092 (host), 29092 (internal) |
| Service Discovery | Eureka Server (Docker: `discovery-server`) | Port 8761 |
| Metrics Store | Prometheus (Docker: `prometheus`) | Port 9090, scrape interval 15s |
| Dashboards | Grafana (Docker: `grafana`) | Port 3000 |

## Database Schema

### Table: `users`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL |
| full_name | VARCHAR(100) | NOT NULL |
| phone_number | VARCHAR(15) | UNIQUE, NOT NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' |
| failed_login_attempts | INT | NOT NULL, DEFAULT 0 |
| locked_until | TIMESTAMP | NULLABLE |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### Table: `bank_accounts`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| account_number | VARCHAR(10) | UNIQUE, NOT NULL |
| user_id | UUID | FK → users(id), NOT NULL |
| balance | BIGINT | NOT NULL, DEFAULT 0 (stored in VND, no decimals) |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'VND' |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' |
| version | BIGINT | NOT NULL, DEFAULT 0 (optimistic locking) |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### Indexes

```sql
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE UNIQUE INDEX idx_users_phone ON users(phone_number);
CREATE UNIQUE INDEX idx_accounts_number ON bank_accounts(account_number);
CREATE INDEX idx_accounts_user_id ON bank_accounts(user_id);
```

## Kafka Configuration

### Topics Produced

| Topic | Partitions | Replication | Retention |
|---|---|---|---|
| account-events | 3 | 1 (dev) | 7 days |

### Topics Consumed

| Topic | Consumer Group | Concurrency |
|---|---|---|
| transfer-events | account-service-group | 3 |

### Serialization

- Key: String (aggregateId)
- Value: JSON (Jackson ObjectMapper with JSR310 module)
- Headers: correlationId, eventType

## Application Configuration

```yaml
server:
  port: 8081
  shutdown: graceful

spring:
  application:
    name: account-service
  datasource:
    url: jdbc:postgresql://localhost:5432/account_db
    username: account_user
    password: account_pass
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: account-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.fintech.common.event

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

jwt:
  secret: ${JWT_SECRET:fintech-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256}
  access-token-expiry: 900000    # 15 minutes in ms
  refresh-token-expiry: 604800000 # 7 days in ms

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: account-service
```

## Flyway Migration Files

```
db/migration/
  V1__create_users_table.sql
  V2__create_bank_accounts_table.sql
  V3__create_indexes.sql
```
