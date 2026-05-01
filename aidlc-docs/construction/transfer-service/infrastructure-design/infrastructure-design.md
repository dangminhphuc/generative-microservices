# Infrastructure Design — Transfer Service

## Infrastructure Mapping

Same as Account Service pattern. Key differences:

| Component | Configuration |
|---|---|
| Application | Port 8082 |
| Database | transfer-db (PostgreSQL), port 5433 |
| Kafka Topics Produced | transfer-events (3 partitions) |
| Kafka Topics Consumed | account-events (consumer group: transfer-service-group) |

## Database Schema

### Table: `transfers`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| source_account_number | VARCHAR(10) | NOT NULL |
| destination_account_number | VARCHAR(10) | NOT NULL |
| amount | BIGINT | NOT NULL (VND, no decimals) |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'VND' |
| description | VARCHAR(200) | NULLABLE |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' |
| user_id | UUID | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| completed_at | TIMESTAMP | NULLABLE |

### Indexes

```sql
CREATE INDEX idx_transfers_source_date ON transfers(source_account_number, created_at);
CREATE INDEX idx_transfers_user_id ON transfers(user_id);
CREATE INDEX idx_transfers_status ON transfers(status);
```

## Application Configuration

```yaml
server:
  port: 8082

spring:
  application:
    name: transfer-service
  datasource:
    url: jdbc:postgresql://localhost:5433/transfer_db
    username: transfer_user
    password: transfer_pass
  kafka:
    consumer:
      group-id: transfer-service-group

account-service:
  url: http://account-service  # Eureka service name
  timeout: 5000

transfer:
  daily-limit: 500000000  # 500M VND
  minimum-amount: 1000     # 1,000 VND
```
