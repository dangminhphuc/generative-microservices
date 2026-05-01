# Unit of Work Plan — Fintech Microservices Platform

## Context
Từ Application Design, hệ thống đã được xác định rõ 3 bounded contexts → 3 business microservices + common module + 2 infrastructure services. Decomposition trực tiếp từ bounded context map.

## Câu Hỏi

## Question 1
Thứ tự phát triển (development order) các units bạn muốn?

A) Account Service trước → Transfer Service → Transaction History Service (theo dependency order)
B) Phát triển song song tất cả services cùng lúc
C) Infrastructure (Gateway + Eureka + Common) trước → rồi business services theo dependency order
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: A

## Question 2
Common module nên chứa những gì?

A) Chỉ shared domain events và DTOs cho inter-service communication
B) Shared events + DTOs + common exceptions + base classes (BaseEntity, BaseValueObject)
C) Minimal — chỉ shared events, mỗi service tự định nghĩa DTOs riêng
X) Other (vui lòng mô tả sau tag [Answer]: bên dưới)

[Answer]: B

---

## Generation Plan

- [x] **Step 1**: Generate unit-of-work.md — định nghĩa các units, responsibilities, code organization
- [x] **Step 2**: Generate unit-of-work-dependency.md — dependency matrix, development order
- [x] **Step 3**: Generate unit-of-work-story-map.md — map stories → units
- [x] **Step 4**: Validate completeness — tất cả stories assigned, dependencies consistent
