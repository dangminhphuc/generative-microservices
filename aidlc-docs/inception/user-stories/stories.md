# User Stories — Fintech Microservices Platform

Tổ chức theo Bounded Context (Domain-Based). Acceptance criteria bao gồm business rules, validation rules, và edge cases.

---

## Bounded Context 1: Account Management

### US-ACC-01: Đăng ký tài khoản người dùng

**As a** Retail Customer,
**I want to** đăng ký tài khoản trên hệ thống,
**So that** tôi có thể sử dụng các dịch vụ ngân hàng.

**Persona**: Minh — Khách hàng cá nhân

**Acceptance Criteria:**

1. **Happy Path — Đăng ký thành công**
   - Given: Người dùng chưa có tài khoản trong hệ thống
   - When: Người dùng cung cấp đầy đủ thông tin hợp lệ (email, password, họ tên, số điện thoại)
   - Then: Hệ thống tạo tài khoản người dùng, trả về user ID, và gửi email xác nhận

2. **Validation — Email không hợp lệ**
   - Given: Người dùng nhập email không đúng format
   - When: Người dùng submit form đăng ký
   - Then: Hệ thống trả về lỗi validation "Email không hợp lệ" (HTTP 400)

3. **Validation — Email đã tồn tại**
   - Given: Email đã được đăng ký trong hệ thống
   - When: Người dùng submit form đăng ký với email đó
   - Then: Hệ thống trả về lỗi "Email đã được sử dụng" (HTTP 409)

4. **Validation — Password yếu**
   - Given: Password không đáp ứng yêu cầu (tối thiểu 8 ký tự, có chữ hoa, chữ thường, số, ký tự đặc biệt)
   - When: Người dùng submit form đăng ký
   - Then: Hệ thống trả về lỗi validation với chi tiết yêu cầu password

5. **Business Rule — Số điện thoại unique**
   - Given: Số điện thoại đã được liên kết với tài khoản khác
   - When: Người dùng submit form đăng ký
   - Then: Hệ thống trả về lỗi "Số điện thoại đã được sử dụng"

6. **Edge Case — Concurrent registration**
   - Given: Hai request đăng ký cùng email đến gần như đồng thời
   - When: Cả hai request được xử lý
   - Then: Chỉ một request thành công, request còn lại nhận lỗi conflict

---

### US-ACC-02: Đăng nhập

**As a** Retail Customer,
**I want to** đăng nhập vào hệ thống,
**So that** tôi có thể truy cập tài khoản và thực hiện giao dịch.

**Persona**: Minh — Khách hàng cá nhân

**Acceptance Criteria:**

1. **Happy Path — Đăng nhập thành công**
   - Given: Người dùng có tài khoản hợp lệ
   - When: Người dùng cung cấp email và password đúng
   - Then: Hệ thống trả về JWT access token và refresh token

2. **Error — Sai credentials**
   - Given: Người dùng nhập sai email hoặc password
   - When: Người dùng submit form đăng nhập
   - Then: Hệ thống trả về lỗi "Email hoặc mật khẩu không đúng" (HTTP 401) — không tiết lộ field nào sai

3. **Business Rule — Brute force protection**
   - Given: Người dùng đã đăng nhập sai 5 lần liên tiếp
   - When: Người dùng thử đăng nhập lần thứ 6
   - Then: Tài khoản bị khóa tạm thời 15 phút, trả về lỗi "Tài khoản tạm khóa"

4. **Business Rule — Token expiration**
   - Given: Access token đã hết hạn
   - When: Người dùng gửi request với expired token
   - Then: Hệ thống trả về HTTP 401, client sử dụng refresh token để lấy access token mới

5. **Edge Case — Refresh token expired**
   - Given: Cả access token và refresh token đều hết hạn
   - When: Người dùng gửi request
   - Then: Hệ thống trả về HTTP 401, người dùng phải đăng nhập lại

---

### US-ACC-03: Tạo tài khoản ngân hàng

**As a** Retail Customer,
**I want to** tạo tài khoản ngân hàng (checking account),
**So that** tôi có thể nhận và chuyển tiền.

**Persona**: Minh — Khách hàng cá nhân

**Acceptance Criteria:**

1. **Happy Path — Tạo tài khoản thành công**
   - Given: Người dùng đã đăng nhập
   - When: Người dùng yêu cầu tạo tài khoản ngân hàng
   - Then: Hệ thống tạo tài khoản với số tài khoản unique, số dư ban đầu = 0, trạng thái ACTIVE

2. **Business Rule — Số tài khoản unique**
   - Given: Hệ thống generate số tài khoản
   - When: Tài khoản được tạo
   - Then: Số tài khoản là duy nhất trong toàn hệ thống, format: 10 chữ số

3. **Business Rule — Giới hạn số tài khoản**
   - Given: Người dùng đã có tối đa số tài khoản cho phép (ví dụ: 5)
   - When: Người dùng yêu cầu tạo thêm tài khoản
   - Then: Hệ thống từ chối với lỗi "Đã đạt giới hạn số tài khoản"

4. **Domain Event — Account Created**
   - Given: Tài khoản được tạo thành công
   - When: Transaction commit
   - Then: Hệ thống publish AccountCreatedEvent qua Kafka

---

### US-ACC-04: Xem số dư tài khoản

**As a** Retail Customer,
**I want to** xem số dư tài khoản của mình,
**So that** tôi biết mình có bao nhiêu tiền.

**Persona**: Minh — Khách hàng cá nhân

**Acceptance Criteria:**

1. **Happy Path — Xem số dư thành công**
   - Given: Người dùng đã đăng nhập và có ít nhất một tài khoản
   - When: Người dùng request xem số dư
   - Then: Hệ thống trả về danh sách tài khoản với số dư hiện tại, số tài khoản, trạng thái

2. **Business Rule — Chỉ xem tài khoản của mình**
   - Given: Người dùng A cố truy cập tài khoản của người dùng B
   - When: Request được gửi
   - Then: Hệ thống trả về HTTP 403 Forbidden

3. **Edge Case — Không có tài khoản**
   - Given: Người dùng chưa tạo tài khoản ngân hàng nào
   - When: Người dùng request xem số dư
   - Then: Hệ thống trả về danh sách rỗng với message hướng dẫn tạo tài khoản

---

## Bounded Context 2: Fund Transfer

### US-TRF-01: Chuyển tiền nội bộ

**As a** Retail Customer,
**I want to** chuyển tiền từ tài khoản của mình sang tài khoản khác trong hệ thống,
**So that** tôi có thể thanh toán hoặc gửi tiền cho người khác.

**Persona**: Minh — Khách hàng cá nhân

**Acceptance Criteria:**

1. **Happy Path — Chuyển tiền thành công**
   - Given: Người dùng có tài khoản ACTIVE với số dư >= số tiền chuyển
   - When: Người dùng yêu cầu chuyển tiền (source account, destination account, amount, description)
   - Then: Hệ thống trừ tiền từ source, cộng tiền vào destination, tạo transaction record, trả về transaction ID

2. **Business Rule — Số dư không đủ**
   - Given: Số dư tài khoản nguồn < số tiền chuyển
   - When: Người dùng yêu cầu chuyển tiền
   - Then: Hệ thống từ chối với lỗi "Số dư không đủ" (HTTP 422)

3. **Business Rule — Số tiền tối thiểu**
   - Given: Số tiền chuyển < 1,000 VND (hoặc minimum amount configured)
   - When: Người dùng yêu cầu chuyển tiền
   - Then: Hệ thống từ chối với lỗi "Số tiền chuyển phải >= 1,000 VND"

4. **Business Rule — Giới hạn chuyển tiền hàng ngày**
   - Given: Tổng số tiền chuyển trong ngày đã đạt giới hạn (ví dụ: 500,000,000 VND)
   - When: Người dùng yêu cầu chuyển thêm
   - Then: Hệ thống từ chối với lỗi "Đã đạt giới hạn chuyển tiền hàng ngày"

5. **Validation — Tài khoản đích không tồn tại**
   - Given: Số tài khoản đích không tồn tại trong hệ thống
   - When: Người dùng yêu cầu chuyển tiền
   - Then: Hệ thống trả về lỗi "Tài khoản đích không tồn tại" (HTTP 404)

6. **Validation — Chuyển cho chính mình**
   - Given: Source account = Destination account
   - When: Người dùng yêu cầu chuyển tiền
   - Then: Hệ thống từ chối với lỗi "Không thể chuyển tiền cho chính mình"

7. **Business Rule — Tài khoản bị khóa**
   - Given: Tài khoản nguồn hoặc đích có trạng thái FROZEN/INACTIVE
   - When: Người dùng yêu cầu chuyển tiền
   - Then: Hệ thống từ chối với lỗi phù hợp

8. **Edge Case — Concurrent transfers**
   - Given: Hai giao dịch chuyển tiền từ cùng tài khoản xảy ra đồng thời
   - When: Cả hai request được xử lý
   - Then: Hệ thống đảm bảo consistency — không cho phép overdraft, sử dụng optimistic locking

9. **Domain Event — Transfer Completed**
   - Given: Giao dịch chuyển tiền thành công
   - When: Transaction commit
   - Then: Hệ thống publish TransferCompletedEvent qua Kafka (chứa transactionId, sourceAccount, destAccount, amount, timestamp)

10. **Domain Event — Transfer Failed**
    - Given: Giao dịch chuyển tiền thất bại (bất kỳ lý do nào)
    - When: Transaction rollback
    - Then: Hệ thống publish TransferFailedEvent qua Kafka (chứa transactionId, reason, timestamp)

---

### US-TRF-02: Xác nhận giao dịch trước khi chuyển

**As a** Retail Customer,
**I want to** xem thông tin xác nhận trước khi thực hiện chuyển tiền,
**So that** tôi có thể kiểm tra lại thông tin và tránh sai sót.

**Persona**: Minh — Khách hàng cá nhân

**Acceptance Criteria:**

1. **Happy Path — Xem thông tin xác nhận**
   - Given: Người dùng đã nhập thông tin chuyển tiền
   - When: Người dùng request preview/confirm
   - Then: Hệ thống trả về: tên chủ tài khoản đích, số tài khoản đích (masked), số tiền, mô tả

2. **Business Rule — Validate trước khi confirm**
   - Given: Thông tin chuyển tiền được gửi để preview
   - When: Hệ thống xử lý preview request
   - Then: Tất cả validation rules (số dư, giới hạn, tài khoản đích) được kiểm tra và báo lỗi nếu có

3. **Edge Case — Số dư thay đổi giữa preview và confirm**
   - Given: Số dư đủ tại thời điểm preview nhưng không đủ tại thời điểm confirm
   - When: Người dùng confirm giao dịch
   - Then: Hệ thống re-validate và trả về lỗi "Số dư không đủ"

---

## Bounded Context 3: Transaction History

### US-TXH-01: Xem lịch sử giao dịch

**As a** Retail Customer,
**I want to** xem lịch sử giao dịch của tài khoản,
**So that** tôi có thể theo dõi các giao dịch đã thực hiện.

**Persona**: Minh — Khách hàng cá nhân

**Acceptance Criteria:**

1. **Happy Path — Xem lịch sử thành công**
   - Given: Người dùng đã đăng nhập và có tài khoản với giao dịch
   - When: Người dùng request lịch sử giao dịch cho một tài khoản
   - Then: Hệ thống trả về danh sách giao dịch (paginated), mỗi giao dịch gồm: transactionId, type (CREDIT/DEBIT), amount, balance after, counterparty, description, timestamp

2. **Business Rule — Pagination**
   - Given: Tài khoản có nhiều giao dịch
   - When: Người dùng request lịch sử
   - Then: Kết quả được phân trang (default 20 items/page), sắp xếp theo thời gian giảm dần (mới nhất trước)

3. **Business Rule — Chỉ xem giao dịch của mình**
   - Given: Người dùng A cố xem lịch sử giao dịch tài khoản của người dùng B
   - When: Request được gửi
   - Then: Hệ thống trả về HTTP 403 Forbidden

4. **Edge Case — Không có giao dịch**
   - Given: Tài khoản chưa có giao dịch nào
   - When: Người dùng request lịch sử
   - Then: Hệ thống trả về danh sách rỗng với total = 0

---

### US-TXH-02: Lọc lịch sử giao dịch

**As a** Retail Customer,
**I want to** lọc lịch sử giao dịch theo thời gian và loại giao dịch,
**So that** tôi có thể tìm nhanh giao dịch cần xem.

**Persona**: Minh — Khách hàng cá nhân

**Acceptance Criteria:**

1. **Happy Path — Lọc theo khoảng thời gian**
   - Given: Người dùng chỉ định startDate và endDate
   - When: Người dùng request lịch sử với filter
   - Then: Hệ thống trả về chỉ các giao dịch trong khoảng thời gian đó

2. **Happy Path — Lọc theo loại giao dịch**
   - Given: Người dùng chỉ định type = CREDIT hoặc DEBIT
   - When: Người dùng request lịch sử với filter
   - Then: Hệ thống trả về chỉ các giao dịch thuộc loại đã chọn

3. **Validation — Khoảng thời gian không hợp lệ**
   - Given: startDate > endDate
   - When: Người dùng request lịch sử
   - Then: Hệ thống trả về lỗi validation "startDate phải trước endDate"

4. **Business Rule — Giới hạn khoảng thời gian**
   - Given: Khoảng thời gian query > 12 tháng
   - When: Người dùng request lịch sử
   - Then: Hệ thống trả về lỗi "Khoảng thời gian tối đa là 12 tháng"

5. **Edge Case — Kết hợp nhiều filter**
   - Given: Người dùng chỉ định cả date range và transaction type
   - When: Người dùng request lịch sử
   - Then: Hệ thống áp dụng tất cả filters (AND logic)

---

### US-TXH-03: Xem chi tiết giao dịch

**As a** Retail Customer,
**I want to** xem chi tiết một giao dịch cụ thể,
**So that** tôi có thể xem đầy đủ thông tin giao dịch.

**Persona**: Minh — Khách hàng cá nhân

**Acceptance Criteria:**

1. **Happy Path — Xem chi tiết thành công**
   - Given: Người dùng có giao dịch với transactionId hợp lệ
   - When: Người dùng request chi tiết giao dịch
   - Then: Hệ thống trả về: transactionId, type, amount, balanceBefore, balanceAfter, sourceAccount, destAccount, description, status, createdAt

2. **Business Rule — Chỉ xem giao dịch liên quan**
   - Given: Người dùng request chi tiết giao dịch không thuộc tài khoản của mình
   - When: Request được gửi
   - Then: Hệ thống trả về HTTP 403 Forbidden

3. **Validation — Transaction không tồn tại**
   - Given: transactionId không tồn tại
   - When: Người dùng request chi tiết
   - Then: Hệ thống trả về HTTP 404 Not Found

---

## Story-Persona Mapping

| Story ID | Story Title | Persona |
|---|---|---|
| US-ACC-01 | Đăng ký tài khoản người dùng | Minh (Retail Customer) |
| US-ACC-02 | Đăng nhập | Minh (Retail Customer) |
| US-ACC-03 | Tạo tài khoản ngân hàng | Minh (Retail Customer) |
| US-ACC-04 | Xem số dư tài khoản | Minh (Retail Customer) |
| US-TRF-01 | Chuyển tiền nội bộ | Minh (Retail Customer) |
| US-TRF-02 | Xác nhận giao dịch trước khi chuyển | Minh (Retail Customer) |
| US-TXH-01 | Xem lịch sử giao dịch | Minh (Retail Customer) |
| US-TXH-02 | Lọc lịch sử giao dịch | Minh (Retail Customer) |
| US-TXH-03 | Xem chi tiết giao dịch | Minh (Retail Customer) |

## INVEST Criteria Verification

| Story | Independent | Negotiable | Valuable | Estimable | Small | Testable |
|---|---|---|---|---|---|---|
| US-ACC-01 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| US-ACC-02 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| US-ACC-03 | ✅ (depends on ACC-01) | ✅ | ✅ | ✅ | ✅ | ✅ |
| US-ACC-04 | ✅ (depends on ACC-03) | ✅ | ✅ | ✅ | ✅ | ✅ |
| US-TRF-01 | ✅ (depends on ACC-03) | ✅ | ✅ | ✅ | ✅ | ✅ |
| US-TRF-02 | ✅ (depends on TRF-01) | ✅ | ✅ | ✅ | ✅ | ✅ |
| US-TXH-01 | ✅ (depends on TRF-01) | ✅ | ✅ | ✅ | ✅ | ✅ |
| US-TXH-02 | ✅ (depends on TXH-01) | ✅ | ✅ | ✅ | ✅ | ✅ |
| US-TXH-03 | ✅ (depends on TXH-01) | ✅ | ✅ | ✅ | ✅ | ✅ |
