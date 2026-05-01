# Fintech Platform

Microservices platform for retail banking — built with Java 21, Spring Boot 3.x, DDD, and Hexagonal Architecture.

## Architecture

- **Microservices**: Account Service, Transfer Service, Transaction History Service
- **Patterns**: DDD (Tactical), Hexagonal Architecture (Ports & Adapters), CQRS-lite
- **Communication**: REST (queries) + Apache Kafka (commands/events)
- **Infrastructure**: Spring Cloud Gateway, Eureka, PostgreSQL (per service), Docker Compose

## Modules

| Module | Port | Description |
|---|---|---|
| `common` | — | Shared events, DTOs, exceptions, base classes |
| `discovery-server` | 8761 | Eureka Service Discovery |
| `api-gateway` | 8080 | Spring Cloud Gateway (routing, JWT, rate limiting) |
| `account-service` | 8081 | User registration, auth, bank account management |
| `transfer-service` | 8082 | Fund transfer, validation, saga orchestration |
| `transaction-history-service` | 8083 | Transaction history query and filtering |

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose

## Quick Start

```bash
# Build all modules
mvn clean package -DskipTests

# Start infrastructure + services
docker compose up -d

# Check Eureka dashboard
open http://localhost:8761

# Check API Gateway health
curl http://localhost:8080/actuator/health
```

## Infrastructure Services

| Service | Port | URL |
|---|---|---|
| Eureka Dashboard | 8761 | http://localhost:8761 |
| API Gateway | 8080 | http://localhost:8080 |
| Prometheus | 9090 | http://localhost:9090 |
| Grafana | 3000 | http://localhost:3000 (admin/admin) |
| Kafka | 9092 | localhost:9092 |
| Account DB | 5432 | localhost:5432 |
| Transfer DB | 5433 | localhost:5433 |
| History DB | 5434 | localhost:5434 |
