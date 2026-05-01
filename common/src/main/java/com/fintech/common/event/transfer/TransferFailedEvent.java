package com.fintech.common.event.transfer;

import com.fintech.common.event.BaseDomainEvent;
import java.math.BigDecimal;
import java.time.Instant;

public class TransferFailedEvent extends BaseDomainEvent {

    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final BigDecimal amount;
    private final String currency;
    private final String reason;
    private final Instant failedAt;

    public TransferFailedEvent(String aggregateId, String sourceAccountNumber,
                                String destinationAccountNumber, BigDecimal amount,
                                String currency, String reason, Instant failedAt) {
        super(aggregateId);
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.failedAt = failedAt;
    }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReason() { return reason; }
    public Instant getFailedAt() { return failedAt; }

    @Override
    public String getEventType() { return "TransferFailed"; }
}
