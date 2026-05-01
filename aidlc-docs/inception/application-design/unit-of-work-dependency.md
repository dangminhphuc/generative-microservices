# Unit of Work Dependencies — Fintech Microservices Platform

## Dependency Matrix

| Unit | Depends On | Dependency Type | Blocking? |
|---|---|---|---|
| Unit 0: Common | None | — | — |
| Unit 0: Discovery Server | None | — | — |
| Unit 0: API Gateway | Discovery Server, all business services | Runtime (routing) | No (can develop in parallel) |
| Unit 1: Account Service | Common | Compile-time (shared classes) | Yes (common must be built first) |
| Unit 2: Transfer Service | Common, Account Service | Compile-time (common) + Runtime (REST client) | Yes (Account Service must exist) |
| Unit 3: Transaction History | Common | Compile-time (common) + Runtime (Kafka events) | No (can develop after common) |

## Development Order (Sequential)

```
Phase 0: Foundation
+--------------------------------------------------+
| Common Module + Discovery Server + API Gateway    |
| (build common first, infra services in parallel)  |
+--------------------------------------------------+
                    |
                    v
Phase 1: Account Service (Unit 1)
+--------------------------------------------------+
| Account Service                                   |
| - User registration, auth, bank account mgmt     |
| - Internal APIs for Transfer Service              |
| - Kafka producer (account events)                 |
+--------------------------------------------------+
                    |
                    v
Phase 2: Transfer Service (Unit 2)
+--------------------------------------------------+
| Transfer Service                                  |
| - Fund transfer, validation, saga                 |
| - REST client to Account Service                  |
| - Kafka producer (transfer events)                |
+--------------------------------------------------+
                    |
                    v
Phase 3: Transaction History Service (Unit 3)
+--------------------------------------------------+
| Transaction History Service                       |
| - Kafka consumer (transfer events)                |
| - Query and filter transactions                   |
+--------------------------------------------------+
```

**Note**: Unit 3 (Transaction History) chỉ depends on Common module (compile-time) và Kafka events (runtime). Có thể phát triển song song với Unit 2 nếu muốn, nhưng theo user preference sẽ phát triển sequential.

## Construction Phase Execution Order

**Step-by-step approach**: Mỗi unit sẽ đi qua full Construction loop. Mỗi stage trong loop sẽ được trình bày để user review và approve trước khi chuyển sang stage tiếp theo. Không tự động chuyển stage.

```
Unit 0 (Foundation):
  Code Generation Plan → [USER REVIEW] → Code Generation → [USER REVIEW]

Unit 1 (Account Service):
  Functional Design → [USER REVIEW] →
  NFR Requirements → [USER REVIEW] →
  NFR Design → [USER REVIEW] →
  Infrastructure Design → [USER REVIEW] →
  Code Generation Plan → [USER REVIEW] →
  Code Generation → [USER REVIEW]

Unit 2 (Transfer Service):
  Functional Design → [USER REVIEW] →
  NFR Requirements → [USER REVIEW] →
  NFR Design → [USER REVIEW] →
  Infrastructure Design → [USER REVIEW] →
  Code Generation Plan → [USER REVIEW] →
  Code Generation → [USER REVIEW]

Unit 3 (Transaction History Service):
  Functional Design → [USER REVIEW] →
  NFR Requirements → [USER REVIEW] →
  NFR Design → [USER REVIEW] →
  Infrastructure Design → [USER REVIEW] →
  Code Generation Plan → [USER REVIEW] →
  Code Generation → [USER REVIEW]

After all units:
  Build and Test → [USER REVIEW]
```

**Nguyên tắc**: Không implement bất kỳ code nào cho đến khi user đã review và approve design artifacts của unit đó.
