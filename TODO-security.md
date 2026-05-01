# Security TODO

## Lỗ hổng: Backend services có thể bị gọi trực tiếp, bypass API Gateway

**Mô tả**: Hiện tại account-service (port 8081) và các service khác expose port trực tiếp. Bất kỳ ai cũng có thể gọi thẳng mà không qua API Gateway, bypass JWT authentication filter. Đặc biệt nguy hiểm với endpoint `/internal/**` (permitAll).

**Giải pháp đề xuất**: Kết hợp 2 cách:

1. **Shared Secret Header (Application-level)**
   - Gateway thêm header bí mật (ví dụ `X-Gateway-Secret`) vào mỗi request forward
   - Backend service thêm filter kiểm tra header — reject nếu thiếu hoặc sai
   - Secret lưu trong config, cần rotate định kỳ

2. **Network Isolation (Infra-level)**
   - Docker Compose: tạo 2 network (`public` + `internal`)
   - Chỉ API Gateway expose port ra ngoài
   - Backend services chỉ giao tiếp trong internal network

**Ưu tiên**: High
**Trạng thái**: Chưa implement
