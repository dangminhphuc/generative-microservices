package com.fintech.common.event.transfer;

import com.fintech.common.event.BaseDomainEvent;
import java.math.BigDecimal;

public class TransferInitiatedEvent extends BaseDomainEvent {

    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final BigDecimal amount;
    private final String currency;
    private final String description;

    public TransferInitiatedEvent(String aggregateId, String sourceAccountNumber,
                                   String destinationAccountNumber, BigDecimal amount,
                                   String currency, String description) {
        super(aggregateId);
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }

    @Override
    public String getEventType() { return "TransferInitiated"; }
}
