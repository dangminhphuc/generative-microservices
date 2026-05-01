# Build Instructions — Fintech Platform

## Prerequisites
- **Java**: 21 (LTS)
- **Maven**: 3.9+
- **Docker**: 24+ with Docker Compose v2
- **Disk Space**: ~2GB (Docker images + Maven cache)

## Build Steps

### 1. Build All Modules
```bash
mvn clean package -DskipTests
```
Expected: BUILD SUCCESS for all 6 modules (common, discovery-server, api-gateway, account-service, transfer-service, transaction-history-service)

### 2. Build with Tests
```bash
mvn clean verify
```
Runs unit tests for all modules. Requires no external services (unit tests use mocks).

### 3. Start Infrastructure (Docker Compose)
```bash
docker compose up -d account-db transfer-db history-db zookeeper kafka prometheus grafana
```
Wait for all infrastructure to be healthy (~30 seconds).

### 4. Start Services (Development Mode)
```bash
# Terminal 1: Discovery Server
cd discovery-server && mvn spring-boot:run

# Terminal 2: Account Service
cd account-service && mvn spring-boot:run

# Terminal 3: Transfer Service
cd transfer-service && mvn spring-boot:run

# Terminal 4: Transaction History Service
cd transaction-history-service && mvn spring-boot:run

# Terminal 5: API Gateway
cd api-gateway && mvn spring-boot:run
```

### 5. Start All via Docker Compose (Alternative)
```bash
docker compose up -d --build
```
Builds all Docker images and starts everything.

### 6. Verify Services
```bash
# Eureka Dashboard
curl http://localhost:8761/eureka/apps

# API Gateway Health
curl http://localhost:8080/actuator/health

# Account Service Health
curl http://localhost:8081/actuator/health

# Transfer Service Health
curl http://localhost:8082/actuator/health

# Transaction History Service Health
curl http://localhost:8083/actuator/health
```

## Build Artifacts

| Module | Artifact | Location |
|---|---|---|
| common | common-0.0.1-SNAPSHOT.jar | common/target/ |
| discovery-server | discovery-server-0.0.1-SNAPSHOT.jar | discovery-server/target/ |
| api-gateway | api-gateway-0.0.1-SNAPSHOT.jar | api-gateway/target/ |
| account-service | account-service-0.0.1-SNAPSHOT.jar | account-service/target/ |
| transfer-service | transfer-service-0.0.1-SNAPSHOT.jar | transfer-service/target/ |
| transaction-history-service | transaction-history-service-0.0.1-SNAPSHOT.jar | transaction-history-service/target/ |

## Troubleshooting

### Maven Build Fails
- Ensure Java 21 is set: `java -version`
- Clear Maven cache: `mvn dependency:purge-local-repository`

### Docker Compose Fails
- Check Docker is running: `docker info`
- Check port conflicts: `lsof -i :5432 -i :9092 -i :8761`
- Reset volumes: `docker compose down -v && docker compose up -d`

### Flyway Migration Fails
- Ensure database is accessible before starting service
- Check connection string in application.yml
- Manual reset: drop and recreate database
