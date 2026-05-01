# Application Design Plan — Fintech Microservices Platform

## Context Summary
- **3 Bounded Contexts**: Account Management, Fund Transfer, Transaction History
- **Architecture**: Hexagonal (Ports & Adapters) per service
- **DDD**: Tactical DDD đầy đủ
- **Communication**: REST (queries) + Kafka (commands/events)
- **Infrastructure**: Spring Cloud Gateway, Eureka, PostgreSQL per service

## Design Plan

- [x] **Step 1**: Define microservice components — boundaries, responsibilities, Hexagonal layers
- [x] **Step 2**: Define component methods — port interfaces (inbound/outbound), application services
- [x] **Step 3**: Define service orchestration — cross-service communication, event flows, saga
- [x] **Step 4**: Define component dependencies — dependency matrix, data flow, event topology
- [x] **Step 5**: Consolidate into application-design.md
- [x] **Step 6**: Validate design completeness and consistency

## Design Decisions (from Requirements + Stories)
- Component boundaries = Bounded Contexts (already defined in stories)
- Hexagonal Architecture layers (already chosen in requirements Q16)
- REST for queries, Kafka events for state changes (already chosen in requirements Q6)
- Database per Service with PostgreSQL (already chosen in requirements Q8-Q9)
- No additional questions needed — all design inputs are available from prior stages
