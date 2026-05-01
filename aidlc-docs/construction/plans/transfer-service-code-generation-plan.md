# Code Generation Plan — Unit 2: Transfer Service

## Unit Context
- **Stories**: US-TRF-01 (Chuyển tiền nội bộ), US-TRF-02 (Xác nhận trước khi chuyển)
- **Dependencies**: common module, Account Service (REST client)
- **Architecture**: Hexagonal, same patterns as Account Service

---

## Generation Steps

### Phase A: Project Setup
- [ ] **Step 1**: Tạo `transfer-service/pom.xml`
- [ ] **Step 2**: Tạo `TransferServiceApplication.java` + `application.yml`
- [ ] **Step 3**: Tạo Flyway migration (V1 transfers table + indexes)

### Phase B: Domain Layer
- [ ] **Step 4**: Tạo `TransferStatus` enum, `Transfer` aggregate root
- [ ] **Step 5**: Tạo `TransferValidationService` (domain service)
- [ ] **Step 6**: Tạo Inbound Ports: `InitiateTransferUseCase`, `PreviewTransferUseCase`
- [ ] **Step 7**: Tạo Outbound Ports: `TransferRepository`, `EventPublisher`, `AccountServicePort`

### Phase C: Application Layer
- [ ] **Step 8**: Tạo DTOs (commands, responses)
- [ ] **Step 9**: Tạo `TransferService` (implements InitiateTransfer + Preview use cases) — US-TRF-01, US-TRF-02
- [ ] **Step 10**: Tạo `TransferEventHandler` (handles AccountDebited/Credited/DebitFailed events)

### Phase D: Infrastructure — Persistence
- [ ] **Step 11**: Tạo `TransferJpaEntity` + `TransferJpaRepository` + `TransferRepositoryAdapter`

### Phase E: Infrastructure — Messaging & REST Client
- [ ] **Step 12**: Tạo `KafkaEventPublisher` + `KafkaConfig`
- [ ] **Step 13**: Tạo `AccountEventConsumer` (Kafka consumer for account events)
- [ ] **Step 14**: Tạo `AccountServiceRestClient` (implements AccountServicePort — REST calls to Account Service)

### Phase F: Infrastructure — REST Adapter
- [ ] **Step 15**: Tạo `TransferController` (POST /transfers, POST /transfers/preview)
- [ ] **Step 16**: Tạo `GlobalExceptionHandler` + `CorrelationIdFilter` + `SecurityConfig`

### Phase G: Configuration & Documentation
- [ ] **Step 17**: Tạo `BeanConfig` + `JpaAuditingConfig`
- [ ] **Step 18**: Tạo code summary

**Estimated**: ~35 Java files
