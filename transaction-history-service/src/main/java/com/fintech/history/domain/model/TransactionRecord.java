package com.fintech.history.domain.model;

import com.fintech.common.domain.BaseEntity;
import com.fintech.common.domain.Money;

import java.time.Instant;

public class TransactionRecord extends BaseEntity {

    private String transactionId;
    private String accountNumber;
    private TransactionType type;
    private Money amount;
    private Money balanceBefore;
    private Money balanceAfter;
    private String counterpartyAccountNumber;
    private String description;
    private String status;

    private TransactionRecord() { super(); }

    public static TransactionRecord create(String transactionId, String accountNumber,
                                            TransactionType type, Money amount,
                                            Money balanceBefore, Money balanceAfter,
                                            String counterpartyAccountNumber,
                                            String description, String status) {
        TransactionRecord record = new TransactionRecord();
        record.transactionId = transactionId;
        record.accountNumber = accountNumber;
        record.type = type;
        record.amount = amount;
        record.balanceBefore = balanceBefore;
        record.balanceAfter = balanceAfter;
        record.counterpartyAccountNumber = counterpartyAccountNumber;
        record.description = description;
        record.status = status;
        return record;
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountNumber() { return accountNumber; }
    public TransactionType getType() { return type; }
    public Money getAmount() { return amount; }
    public Money getBalanceBefore() { return balanceBefore; }
    public Money getBalanceAfter() { return balanceAfter; }
    public String getCounterpartyAccountNumber() { return counterpartyAccountNumber; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
}
