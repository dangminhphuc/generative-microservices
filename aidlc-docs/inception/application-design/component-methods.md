# Component Methods — Fintech Microservices Platform

Định nghĩa method signatures cho mỗi component. Business rules chi tiết sẽ được định nghĩa trong Functional Design.

---

## Account Service

### Inbound Ports (Use Cases)

```java
// RegisterUserUseCase
RegisterUserResponse execute(RegisterUserCommand command)
// Input: email, password, fullName, phoneNumber
// Output: userId, email, createdAt
// Errors: EmailAlreadyExistsException, PhoneAlreadyExistsException, ValidationException

// LoginUseCase
LoginResponse execute(LoginCommand command)
// Input: email, password
// Output: accessToken, refreshToken, expiresIn
// Errors: InvalidCredentialsException, AccountLockedException

// RefreshTokenUseCase
LoginResponse execute(RefreshTokenCommand command)
// Input: refreshToken
// Output: accessToken, refreshToken, expiresIn
// Errors: InvalidTokenException, TokenExpiredException

// CreateBankAccountUseCase
CreateBankAccountResponse execute(CreateBankAccountCommand command)
// Input: userId
// Output: accountId, accountNumber, balance (0), status (ACTIVE)
// Errors: MaxAccountsReachedException, UserNotFoundException

// GetBalanceUseCase
BalanceResponse execute(GetBalanceQuery query)
// Input: userId, accountId (optional — all accounts if omitted)
// Output: List<AccountBalance> (accountId, accountNumber, balance, status)
// Errors: AccountNotFoundException, AccessDeniedException

// GetAccountByNumberUseCase (internal — called by Transfer Service)
AccountInfoResponse execute(GetAccountByNumberQuery query)
// Input: accountNumber
// Output: accountId, accountNumber, ownerName, status
// Errors: AccountNotFoundException
```

### Outbound Ports

```java
// UserRepository
User save(User user)
Optional<User> findByEmail(Email email)
Optional<User> findByPhoneNumber(PhoneNumber phone)
boolean existsByEmail(Email email)

// BankAccountRepository
BankAccount save(BankAccount account)
Optional<BankAccount> findById(AccountId id)
Optional<BankAccount> findByAccountNumber(AccountNumber number)
List<BankAccount> findByUserId(UserId userId)
int countByUserId(UserId userId)

// EventPublisher
void publish(DomainEvent event)

// PasswordEncoder
String encode(String rawPassword)
boolean matches(String rawPassword, String encodedPassword)
```

---

## Transfer Service

### Inbound Ports (Use Cases)

```java
// PreviewTransferUseCase
TransferPreviewResponse execute(PreviewTransferCommand command)
// Input: sourceAccountNumber, destAccountNumber, amount, description
// Output: sourceAccountName, destAccountName (masked), amount, description
// Errors: AccountNotFoundException, InsufficientBalanceException, ValidationException

// InitiateTransferUseCase
TransferResponse execute(InitiateTransferCommand command)
// Input: userId, sourceAccountNumber, destAccountNumber, amount, description
// Output: transferId, status, amount, createdAt
// Errors: InsufficientBalanceException, DailyLimitExceededException,
//         MinimumAmountException, SameAccountException,
//         AccountFrozenException, AccountNotFoundException

// GetTransferStatusUseCase
TransferStatusResponse execute(GetTransferStatusQuery query)
// Input: transferId
// Output: transferId, status, amount, sourceAccount, destAccount, createdAt, completedAt
// Errors: TransferNotFoundException, AccessDeniedException
```

### Outbound Ports

```java
// TransferRepository
Transfer save(Transfer transfer)
Optional<Transfer> findById(TransferId id)
Money sumAmountBySourceAccountAndDateRange(AccountNumber source, LocalDate date)

// AccountServicePort (REST client → Account Service)
AccountInfoResponse getAccountByNumber(AccountNumber number)
BalanceResponse getBalance(AccountNumber number)
void debit(AccountNumber number, Money amount, TransferId transferId)
void credit(AccountNumber number, Money amount, TransferId transferId)

// EventPublisher
void publish(DomainEvent event)
```

---

## Transaction History Service

### Inbound Ports (Use Cases)

```java
// GetTransactionHistoryUseCase
Page<TransactionRecordResponse> execute(GetTransactionHistoryQuery query)
// Input: accountNumber, page, size (default 20)
// Output: Paginated list of transactions (sorted by timestamp DESC)
// Errors: AccessDeniedException

// FilterTransactionsUseCase
Page<TransactionRecordResponse> execute(FilterTransactionsQuery query)
// Input: accountNumber, startDate, endDate, transactionType (optional), page, size
// Output: Filtered paginated list
// Errors: InvalidDateRangeException, DateRangeTooLargeException, AccessDeniedException

// GetTransactionDetailUseCase
TransactionDetailResponse execute(GetTransactionDetailQuery query)
// Input: transactionId
// Output: Full transaction detail (transactionId, type, amount, balanceBefore, balanceAfter,
//         sourceAccount, destAccount, description, status, createdAt)
// Errors: TransactionNotFoundException, AccessDeniedException
```

### Outbound Ports

```java
// TransactionRecordRepository
TransactionRecord save(TransactionRecord record)
Optional<TransactionRecord> findById(TransactionId id)
Page<TransactionRecord> findByAccountNumber(AccountNumber number, Pageable pageable)
Page<TransactionRecord> findByAccountNumberAndFilters(
    AccountNumber number, LocalDate startDate, LocalDate endDate,
    TransactionType type, Pageable pageable)
```

### Kafka Event Handlers (Inbound Adapter)

```java
// TransferEventConsumer
void handleTransferCompleted(TransferCompletedEvent event)
// Creates 2 TransactionRecords: DEBIT for source, CREDIT for destination

void handleTransferFailed(TransferFailedEvent event)
// Creates failed transaction record for audit
```
