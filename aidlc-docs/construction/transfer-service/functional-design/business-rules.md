# Business Rules — Transfer Service

## BR-TRF-01: Initiate Transfer

| Rule ID | Rule | Validation |
|---|---|---|
| BR-TRF-01.1 | Amount >= 1,000 VND | TransferValidationService.validateMinimumAmount() |
| BR-TRF-01.2 | Source != Destination | TransferValidationService.validateNotSameAccount() |
| BR-TRF-01.3 | Source account must exist and be ACTIVE | REST call to Account Service |
| BR-TRF-01.4 | Destination account must exist | REST call to Account Service |
| BR-TRF-01.5 | Source account balance >= amount | REST call to Account Service |
| BR-TRF-01.6 | Daily transfer limit <= 500,000,000 VND | Sum today's transfers from TransferRepository |
| BR-TRF-01.7 | Source account belongs to requesting user | userId from JWT must own source account |
| BR-TRF-01.8 | On success: create Transfer (PENDING), publish TransferInitiatedEvent | Kafka event |

## BR-TRF-02: Preview Transfer

| Rule ID | Rule | Validation |
|---|---|---|
| BR-TRF-02.1 | All validation rules from BR-TRF-01 apply | Same validation, no state change |
| BR-TRF-02.2 | Return destination account owner name (masked) | REST call to Account Service |
| BR-TRF-02.3 | Balance may change between preview and confirm | Re-validate on confirm |

## BR-TRF-03: Transfer Status Updates (Event-driven)

| Rule ID | Rule | Trigger |
|---|---|---|
| BR-TRF-03.1 | On AccountDebitedEvent + AccountCreditedEvent: mark COMPLETED | Kafka consumer |
| BR-TRF-03.2 | On DebitFailedEvent: mark FAILED with reason | Kafka consumer |
| BR-TRF-03.3 | Publish TransferCompletedEvent on COMPLETED | Kafka producer |
| BR-TRF-03.4 | Publish TransferFailedEvent on FAILED | Kafka producer |

## Error Handling

| Error | HTTP Status | Error Code | Message |
|---|---|---|---|
| Amount < minimum | 422 | MINIMUM_AMOUNT | Số tiền chuyển phải >= 1,000 VND |
| Same account | 422 | SAME_ACCOUNT | Không thể chuyển tiền cho chính mình |
| Source not found | 404 | ACCOUNT_NOT_FOUND | Tài khoản nguồn không tồn tại |
| Dest not found | 404 | ACCOUNT_NOT_FOUND | Tài khoản đích không tồn tại |
| Insufficient balance | 422 | INSUFFICIENT_BALANCE | Số dư không đủ |
| Daily limit exceeded | 422 | DAILY_LIMIT_EXCEEDED | Đã đạt giới hạn chuyển tiền hàng ngày |
| Account frozen | 422 | ACCOUNT_FROZEN | Tài khoản đã bị đóng băng |
| Access denied | 403 | ACCESS_DENIED | Không có quyền truy cập |
