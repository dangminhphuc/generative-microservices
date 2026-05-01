package com.fintech.transfer.domain.model;

import com.fintech.common.domain.BaseEntity;
import com.fintech.common.domain.Money;

import java.time.Instant;
import java.util.UUID;

public class Transfer extends BaseEntity {

    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private Money amount;
    private String description;
    private TransferStatus status;
    private UUID userId;
    private Instant completedAt;

    private Transfer() { super(); }

    public static Transfer create(String sourceAccountNumber, String destinationAccountNumber,
                                   Money amount, String description, UUID userId) {
        Transfer transfer = new Transfer();
        transfer.sourceAccountNumber = sourceAccountNumber;
        transfer.destinationAccountNumber = destinationAccountNumber;
        transfer.amount = amount;
        transfer.description = description;
        transfer.status = TransferStatus.PENDING;
        transfer.userId = userId;
        transfer.completedAt = null;
        return transfer;
    }

    public void markCompleted() {
        if (this.status != TransferStatus.PENDING) {
            return; // idempotent — already in terminal state
        }
        this.status = TransferStatus.COMPLETED;
        this.completedAt = Instant.now();
        markUpdated();
    }

    public void markFailed() {
        if (this.status != TransferStatus.PENDING) {
            return; // idempotent
        }
        this.status = TransferStatus.FAILED;
        this.completedAt = Instant.now();
        markUpdated();
    }

    public boolean isTerminal() {
        return status == TransferStatus.COMPLETED || status == TransferStatus.FAILED;
    }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public TransferStatus getStatus() { return status; }
    public UUID getUserId() { return userId; }
    public Instant getCompletedAt() { return completedAt; }
}
