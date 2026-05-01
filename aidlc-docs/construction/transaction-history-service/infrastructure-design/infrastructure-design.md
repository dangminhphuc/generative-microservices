# Infrastructure Design — Transaction History Service

## Infrastructure Mapping

| Component | Configuration |
|---|---|
| Application | Port 8083 |
| Database | history-db (PostgreSQL), port 5434 |
| Kafka Topics Consumed | transfer-events (consumer group: history-service-group) |
| Kafka Topics Produced | None |

## Database Schema

### Table: `transaction_records`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| transaction_id | VARCHAR(36) | NOT NULL (transfer ID) |
| account_number | VARCHAR(10) | NOT NULL |
| type | VARCHAR(10) | NOT NULL (CREDIT/DEBIT) |
| amount | BIGINT | NOT NULL |
| currency | VARCHAR(3) | NOT NULL DEFAULT 'VND' |
| balance_before | BIGINT | NOT NULL |
| balance_after | BIGINT | NOT NULL |
| counterparty_account_number | VARCHAR(10) | NOT NULL |
| description | VARCHAR(200) | NULLABLE |
| status | VARCHAR(20) | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |

### Indexes
```sql
CREATE INDEX idx_txrecords_account_date ON transaction_records(account_number, created_at DESC);
CREATE INDEX idx_txrecords_transaction_id ON transaction_records(transaction_id);
```

## Application Configuration
```yaml
server:
  port: 8083
spring:
  application:
    name: transaction-history-service
  datasource:
    url: jdbc:postgresql://localhost:5434/history_db
    username: history_user
    password: history_pass
  kafka:
    consumer:
      group-id: history-service-group
```
