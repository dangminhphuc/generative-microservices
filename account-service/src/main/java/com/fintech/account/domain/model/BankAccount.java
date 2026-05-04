package com.fintech.account.domain.model;

import com.fintech.common.domain.BaseEntity;
import com.fintech.common.domain.Money;
import com.fintech.common.exception.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;

public class BankAccount extends BaseEntity {

    private AccountNumber accountNumber;
    private UUID userId;
    private Money balance;
    private AccountStatus status;
    private long version;

    private BankAccount() { super(); }

    private BankAccount(UUID id) { super(id); }

    public static BankAccount reconstitute(UUID id, AccountNumber accountNumber, UUID userId,
                                            Money balance, AccountStatus status, long version,
                                            Instant createdAt, Instant updatedAt) {
        BankAccount account = new BankAccount(id);
        account.accountNumber = accountNumber;
        account.userId = userId;
        account.balance = balance;
        account.status = status;
        account.version = version;
        account.setCreatedAt(createdAt);
        account.setUpdatedAt(updatedAt);
        return account;
    }

    public static BankAccount create(AccountNumber accountNumber, UUID userId) {
        BankAccount account = new BankAccount();
        account.accountNumber = accountNumber;
        account.userId = userId;
        account.balance = Money.zero();
        account.status = AccountStatus.ACTIVE;
        account.version = 0;
        return account;
    }

    public void debit(Money amount, String transferId) {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account is not active", "ACCOUNT_NOT_ACTIVE");
        }
        if (balance.isLessThan(amount)) {
            throw new BusinessRuleException("Insufficient balance", "INSUFFICIENT_BALANCE");
        }
        this.balance = this.balance.subtract(amount);
        markUpdated();
    }

    public void credit(Money amount, String transferId) {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account is not active", "ACCOUNT_NOT_ACTIVE");
        }
        this.balance = this.balance.add(amount);
        markUpdated();
    }

    public AccountNumber getAccountNumber() { return accountNumber; }
    public UUID getUserId() { return userId; }
    public Money getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public long getVersion() { return version; }
}
