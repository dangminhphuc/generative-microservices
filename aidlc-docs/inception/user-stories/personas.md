# User Personas — Fintech Microservices Platform

---

## Persona 1: Retail Customer (Khách hàng cá nhân)

| Attribute | Detail |
|---|---|
| **Name** | Minh — Khách hàng cá nhân |
| **Age Range** | 22-45 |
| **Tech Savvy** | Trung bình đến cao |
| **Goals** | Quản lý tài khoản cá nhân, chuyển tiền nhanh chóng, theo dõi lịch sử giao dịch |
| **Frustrations** | Giao dịch chậm, không rõ trạng thái giao dịch, khó tìm lịch sử giao dịch cũ |
| **Motivations** | Tiện lợi, nhanh chóng, an toàn |
| **Usage Frequency** | Hàng ngày đến hàng tuần |

### User Journey
1. Đăng ký tài khoản → Xác thực danh tính
2. Tạo tài khoản ngân hàng (checking account)
3. Xem số dư tài khoản
4. Chuyển tiền nội bộ cho người dùng khác trong hệ thống
5. Xem lịch sử giao dịch, lọc theo thời gian/loại giao dịch

### Relevant Bounded Contexts
- Account Management (primary)
- Fund Transfer (primary)
- Transaction History (primary)

---

## Persona 2: System (Hệ thống tự động)

| Attribute | Detail |
|---|---|
| **Name** | System — Automated Processes |
| **Type** | Non-human actor |
| **Goals** | Xử lý giao dịch bất đồng bộ, gửi thông báo, đảm bảo tính nhất quán dữ liệu |
| **Responsibilities** | Event processing, saga orchestration, notification delivery |

### Relevant Bounded Contexts
- Fund Transfer (event processing, saga)
- Transaction History (event consumption, record creation)

