package com.fintech.common.event.account;

import com.fintech.common.event.BaseDomainEvent;
import java.math.BigDecimal;

public class AccountCreditedEvent extends BaseDomainEvent {

    private final String accountNumber;
    private final BigDecimal amount;
    private final String currency;
    private final String transferId;

    public AccountCreditedEvent(String aggregateId, String accountNumber,
                                BigDecimal amount, String currency, String transferId) {
        super(aggregateId);
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.currency = currency;
        this.transferId = transferId;
    }

    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getTransferId() { return transferId; }

    @Override
    public String getEventType() { return "AccountCredited"; }
}
