# Code Summary — Unit 0: Foundation

## Files Generated

### Parent POM
- `pom.xml` — Maven multi-module parent (Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0)

### Common Module (11 files)
- `common/pom.xml`
- `common/src/.../domain/BaseEntity.java` — Abstract base entity (UUID id, timestamps)
- `common/src/.../domain/BaseValueObject.java` — Abstract base for value objects
- `common/src/.../domain/Money.java` — Money value object (BigDecimal + currency, arithmetic)
- `common/src/.../event/BaseDomainEvent.java` — Abstract base domain event
- `common/src/.../event/account/AccountCreatedEvent.java`
- `common/src/.../event/account/AccountDebitedEvent.java`
- `common/src/.../event/account/AccountCreditedEvent.java`
- `common/src/.../event/account/DebitFailedEvent.java`
- `common/src/.../event/transfer/TransferInitiatedEvent.java`
- `common/src/.../event/transfer/TransferCompletedEvent.java`
- `common/src/.../event/transfer/TransferFailedEvent.java`
- `common/src/.../dto/AccountInfoResponse.java` — Java record
- `common/src/.../dto/BalanceResponse.java` — Java record with nested AccountBalance
- `common/src/.../exception/BaseException.java`
- `common/src/.../exception/ResourceNotFoundException.java`
- `common/src/.../exception/AccessDeniedException.java`
- `common/src/.../exception/ValidationException.java`
- `common/src/.../exception/BusinessRuleException.java`

### Discovery Server (3 files)
- `discovery-server/pom.xml`
- `discovery-server/src/.../DiscoveryServerApplication.java`
- `discovery-server/src/main/resources/application.yml`

### API Gateway (5 files)
- `api-gateway/pom.xml`
- `api-gateway/src/.../ApiGatewayApplication.java`
- `api-gateway/src/.../filter/JwtAuthenticationFilter.java`
- `api-gateway/src/.../config/SecurityConfig.java`
- `api-gateway/src/main/resources/application.yml`

### Docker & Infrastructure (8 files)
- `docker-compose.yml` — Full stack (3x PostgreSQL, Kafka, Zookeeper, Prometheus, Grafana, all services)
- `infra/prometheus/prometheus.yml`
- `discovery-server/Dockerfile`
- `api-gateway/Dockerfile`
- `account-service/Dockerfile`
- `transfer-service/Dockerfile`
- `transaction-history-service/Dockerfile`
- `README.md`

**Total: ~30 files**
