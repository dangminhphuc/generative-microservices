package com.fintech.account.infrastructure.adapter.out.persistence;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.AccountStatus;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.common.domain.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 10)
    private String accountNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private long balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BankAccountJpaEntity() {}

    public static BankAccountJpaEntity fromDomain(BankAccount account) {
        BankAccountJpaEntity entity = new BankAccountJpaEntity();
        entity.id = account.getId();
        entity.accountNumber = account.getAccountNumber().getValue();
        entity.userId = account.getUserId();
        entity.balance = account.getBalance().getAmount().longValue();
        entity.currency = account.getBalance().getCurrency();
        entity.status = account.getStatus();
        entity.version = account.getVersion();
        entity.createdAt = account.getCreatedAt();
        entity.updatedAt = account.getUpdatedAt();
        return entity;
    }

    public BankAccount toDomain() {
        // Reconstitute via factory — simplified for now
        BankAccount account = BankAccount.create(
                AccountNumber.of(accountNumber), userId);
        // Note: In production, use a proper reconstitution method
        // that sets all fields including balance, version, timestamps
        return account;
    }

    // Getters for JPA queries
    public UUID getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public UUID getUserId() { return userId; }
    public long getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
