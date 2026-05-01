# Deployment Architecture — Account Service

## Docker Compose Deployment

```
+------------------------------------------------------------------+
|                     Docker Compose Network                       |
|                                                                  |
|  +------------------+    +------------------+                    |
|  | discovery-server |    |   api-gateway    |                    |
|  | (Eureka) :8761   |<---| :8080            |                    |
|  +------------------+    +------------------+                    |
|          ^                       |                               |
|          |                       | /api/accounts/**, /api/auth/**|
|          |                       v                               |
|  +------------------+    +------------------+                    |
|  |   account-db     |<---|  account-service |                    |
|  | (PostgreSQL)     |    |  :8081           |                    |
|  | :5432            |    +------------------+                    |
|  +------------------+           |    ^                           |
|                                 |    |                           |
|                                 v    |                           |
|                          +------------------+                    |
|                          |     kafka        |                    |
|                          |  :9092 / :29092  |                    |
|                          +------------------+                    |
|                                 ^                                |
|                                 |                                |
|                          +------------------+                    |
|                          |   zookeeper      |                    |
|                          |  :2181           |                    |
|                          +------------------+                    |
|                                                                  |
|  +------------------+    +------------------+                    |
|  |   prometheus     |    |    grafana       |                    |
|  |  :9090           |    |  :3000           |                    |
|  +------------------+    +------------------+                    |
+------------------------------------------------------------------+
```

## Container Specifications

| Container | Image | Memory | CPU | Health Check |
|---|---|---|---|---|
| account-service | fintech/account-service:latest | 512MB | 0.5 | /actuator/health |
| account-db | postgres:16-alpine | 256MB | 0.25 | pg_isready |
| kafka | confluentinc/cp-kafka:7.7.0 | 512MB | 0.5 | kafka-broker-api-versions |
| zookeeper | confluentinc/cp-zookeeper:7.7.0 | 256MB | 0.25 | ruok |

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| SPRING_DATASOURCE_URL | jdbc:postgresql://account-db:5432/account_db | DB connection |
| SPRING_DATASOURCE_USERNAME | account_user | DB user |
| SPRING_DATASOURCE_PASSWORD | account_pass | DB password |
| SPRING_KAFKA_BOOTSTRAP_SERVERS | kafka:29092 | Kafka broker |
| EUREKA_CLIENT_SERVICEURL_DEFAULTZONE | http://discovery-server:8761/eureka/ | Eureka |
| JWT_SECRET | (configured) | JWT signing key |
| SERVER_PORT | 8081 | Service port |

## Startup Dependencies

```
1. account-db (PostgreSQL) → must be ready
2. kafka + zookeeper → must be ready
3. discovery-server (Eureka) → must be ready
4. account-service → starts, runs Flyway migrations, registers with Eureka
```

Docker Compose `depends_on` ensures ordering. Service uses Spring retry for Eureka/Kafka connection on startup.

## Data Persistence

| Data | Storage | Persistence |
|---|---|---|
| User data | account-db (PostgreSQL) | Docker volume: `account-db-data` |
| Kafka events | Kafka broker | Kafka log retention: 7 days |
| Metrics | Prometheus | In-memory (development), configurable retention |
