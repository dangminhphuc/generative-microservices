package com.fintech.common.event.account;

import com.fintech.common.event.BaseDomainEvent;

public class AccountCreatedEvent extends BaseDomainEvent {

    private final String accountNumber;
    private final String userId;

    public AccountCreatedEvent(String aggregateId, String accountNumber, String userId) {
        super(aggregateId);
        this.accountNumber = accountNumber;
        this.userId = userId;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getUserId() { return userId; }

    @Override
    public String getEventType() { return "AccountCreated"; }
}
