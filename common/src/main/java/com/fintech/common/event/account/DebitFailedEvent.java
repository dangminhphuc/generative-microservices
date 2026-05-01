package com.fintech.common.event.account;

import com.fintech.common.event.BaseDomainEvent;
import java.math.BigDecimal;

public class DebitFailedEvent extends BaseDomainEvent {

    private final String accountNumber;
    private final BigDecimal amount;
    private final String currency;
    private final String transferId;
    private final String reason;

    public DebitFailedEvent(String aggregateId, String accountNumber,
                            BigDecimal amount, String currency,
                            String transferId, String reason) {
        super(aggregateId);
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.currency = currency;
        this.transferId = transferId;
        this.reason = reason;
    }

    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getTransferId() { return transferId; }
    public String getReason() { return reason; }

    @Override
    public String getEventType() { return "DebitFailed"; }
}
