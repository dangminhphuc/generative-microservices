# Component Dependencies — Fintech Microservices Platform

---

## Dependency Matrix

| Component | Depends On | Communication | Coupling Level |
|---|---|---|---|
| API Gateway | Eureka, Account Service, Transfer Service, Transaction History Service | REST (routing) | Loose (routing only) |
| Account Service | Eureka, PostgreSQL (account-db), Kafka | REST (inbound), Kafka (pub/sub) | Low |
| Transfer Service | Eureka, PostgreSQL (transfer-db), Kafka, Account Service | REST (inbound + outbound to Account), Kafka (pub) | Medium (depends on Account Service) |
| Transaction History Service | Eureka, PostgreSQL (history-db), Kafka | REST (inbound), Kafka (consume only) | Low (event consumer) |
| Discovery Server (Eureka) | None | REST (service registry) | None |

---

## Data Flow Diagram

```
+------------------+
|     Client       |
+------------------+
        |
        | HTTPS
        v
+------------------+       +------------------+
|   API Gateway    |------>| Discovery Server |
| (Spring Cloud)   |<------| (Eureka)         |
+------------------+       +------------------+
   |       |       |              ^  ^  ^
   |       |       |              |  |  |
   v       v       v              |  |  |
+------+ +------+ +------+       |  |  |
| Acct | | Trf  | | TxH  |-------+--+--+
| Svc  | | Svc  | | Svc  | (register)
+------+ +------+ +------+
   |    /  |   \     |
   |   /   |    \    |
   v  v    v     v   v
+------+ +------+ +------+
|acct  | |trf   | |hist  |
|  db  | |  db  | |  db  |
+------+ +------+ +------+
   (PostgreSQL instances)

Transfer Svc --REST--> Account Svc (verify account/balance)
Account Svc  --Kafka--> transfer-events topic
Transfer Svc --Kafka--> transfer-events topic
TxH Svc      <--Kafka-- transfer-events topic (consume)
```

---

## Service Startup Order

1. **Discovery Server (Eureka)** — phải khởi động trước
2. **PostgreSQL databases** (3 instances) — phải sẵn sàng trước services
3. **Apache Kafka + Zookeeper** — phải sẵn sàng trước services
4. **Account Service** — khởi động trước Transfer Service (dependency)
5. **Transfer Service** — depends on Account Service
6. **Transaction History Service** — independent, có thể khởi động song song với Transfer
7. **API Gateway** — khởi động cuối cùng, sau khi tất cả services đã register với Eureka

---

## Maven Module Structure

```
fintech-platform/                    (parent POM)
+-- pom.xml
+-- common/                          (shared module)
|   +-- pom.xml
|   +-- src/
|       +-- events/                  (shared domain events)
|       +-- dto/                     (shared DTOs for inter-service)
|       +-- exception/               (common exceptions)
+-- discovery-server/                (Eureka Server)
|   +-- pom.xml
+-- api-gateway/                     (Spring Cloud Gateway)
|   +-- pom.xml
+-- account-service/                 (Account bounded context)
|   +-- pom.xml
|   +-- src/
|       +-- domain/
|       +-- application/
|       +-- infrastructure/
+-- transfer-service/                (Transfer bounded context)
|   +-- pom.xml
|   +-- src/
|       +-- domain/
|       +-- application/
|       +-- infrastructure/
+-- transaction-history-service/     (Transaction History bounded context)
|   +-- pom.xml
|   +-- src/
|       +-- domain/
|       +-- application/
|       +-- infrastructure/
+-- docker-compose.yml
```
