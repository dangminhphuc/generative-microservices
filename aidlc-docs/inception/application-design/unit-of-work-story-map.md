# Unit of Work — Story Map

## Story Assignment

| Story ID | Story Title | Unit | Bounded Context |
|---|---|---|---|
| US-ACC-01 | Đăng ký tài khoản người dùng | Unit 1: Account Service | Account Management |
| US-ACC-02 | Đăng nhập | Unit 1: Account Service | Account Management |
| US-ACC-03 | Tạo tài khoản ngân hàng | Unit 1: Account Service | Account Management |
| US-ACC-04 | Xem số dư tài khoản | Unit 1: Account Service | Account Management |
| US-TRF-01 | Chuyển tiền nội bộ | Unit 2: Transfer Service | Fund Transfer |
| US-TRF-02 | Xác nhận giao dịch trước khi chuyển | Unit 2: Transfer Service | Fund Transfer |
| US-TXH-01 | Xem lịch sử giao dịch | Unit 3: Transaction History Service | Transaction History |
| US-TXH-02 | Lọc lịch sử giao dịch | Unit 3: Transaction History Service | Transaction History |
| US-TXH-03 | Xem chi tiết giao dịch | Unit 3: Transaction History Service | Transaction History |

## Coverage Summary

| Unit | Stories | Acceptance Criteria Total |
|---|---|---|
| Unit 0: Infrastructure & Common | 0 (foundation) | — |
| Unit 1: Account Service | 4 stories | 19 criteria |
| Unit 2: Transfer Service | 2 stories | 13 criteria |
| Unit 3: Transaction History Service | 3 stories | 13 criteria |
| **Total** | **9 stories** | **45 criteria** |

## Validation
- ✅ All 9 user stories assigned to a unit
- ✅ No story assigned to multiple units
- ✅ Each unit has clear bounded context alignment
- ✅ Dependencies between units match Application Design
- ✅ Development order respects dependency constraints
