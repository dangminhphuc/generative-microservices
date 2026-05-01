# Code Summary — Unit 2: Transfer Service

## Files Generated (~30 files)

### Project Setup (4 files)
- `transfer-service/pom.xml`
- `TransferServiceApplication.java`
- `application.yml`
- `db/migration/V1__create_transfers_table.sql`

### Domain Layer (8 files)
- `TransferStatus` enum, `Transfer` aggregate root
- `TransferValidationService` domain service
- Inbound Ports: `InitiateTransferUseCase`, `PreviewTransferUseCase`
- Outbound Ports: `TransferRepository`, `EventPublisher`, `AccountServicePort`

### Application Layer (6 files)
- DTOs: `InitiateTransferCommand`, `PreviewTransferCommand`, `TransferResponse`, `TransferPreviewResponse`
- Services: `TransferService`, `TransferEventHandler`

### Infrastructure — Persistence (3 files)
- `TransferJpaEntity`, `TransferJpaRepository`, `TransferRepositoryAdapter`

### Infrastructure — Messaging & REST Client (3 files)
- `KafkaEventPublisher`, `AccountEventConsumer`, `AccountServiceRestClient`

### Infrastructure — REST & Cross-Cutting (5 files)
- `TransferController`, `GlobalExceptionHandler`, `SecurityConfig`, `CorrelationIdFilter`, `BeanConfig`

## Story Coverage
- ✅ US-TRF-01: Chuyển tiền nội bộ
- ✅ US-TRF-02: Xác nhận giao dịch trước khi chuyển
