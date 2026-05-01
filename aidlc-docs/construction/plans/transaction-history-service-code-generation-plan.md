# Code Generation Plan — Unit 3: Transaction History Service

## Unit Context
- **Stories**: US-TXH-01 (Xem lịch sử), US-TXH-02 (Lọc giao dịch), US-TXH-03 (Xem chi tiết)
- **Type**: Event consumer + Query API (simplest service)
- **Dependencies**: common module only (no REST dependency on other services)

---

## Generation Steps

### Phase A: Project Setup
- [ ] **Step 1**: Tạo POM + main class + application.yml
- [ ] **Step 2**: Tạo Flyway migration (transaction_records table + indexes)

### Phase B: Domain Layer
- [ ] **Step 3**: Tạo `TransactionType` enum, `TransactionRecord` aggregate
- [ ] **Step 4**: Tạo Inbound Ports + Outbound Ports

### Phase C: Application Layer
- [ ] **Step 5**: Tạo DTOs (query, response)
- [ ] **Step 6**: Tạo `TransactionHistoryService` (implements all 3 query use cases)
- [ ] **Step 7**: Tạo `TransferEventRecorder` (handles Kafka events → creates records)

### Phase D: Infrastructure
- [ ] **Step 8**: Tạo JPA entity + repository + adapter
- [ ] **Step 9**: Tạo Kafka consumer (`TransferEventConsumer`)
- [ ] **Step 10**: Tạo `TransactionHistoryController` (GET endpoints)
- [ ] **Step 11**: Tạo cross-cutting (exception handler, security, correlation, config)

### Phase E: Documentation
- [ ] **Step 12**: Tạo code summary

**Estimated**: ~25 files
