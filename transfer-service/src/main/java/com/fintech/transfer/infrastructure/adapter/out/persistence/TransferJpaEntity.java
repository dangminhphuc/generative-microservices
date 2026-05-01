package com.fintech.transfer.infrastructure.adapter.out.persistence;

import com.fintech.transfer.domain.model.TransferStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class TransferJpaEntity {

    @Id
    private UUID id;

    @Column(name = "source_account_number", nullable = false, length = 10)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", nullable = false, length = 10)
    private String destinationAccountNumber;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected TransferJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public TransferStatus getStatus() { return status; }
    public UUID getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }

    // Setters for adapter mapping
    public void setId(UUID id) { this.id = id; }
    public void setSourceAccountNumber(String s) { this.sourceAccountNumber = s; }
    public void setDestinationAccountNumber(String d) { this.destinationAccountNumber = d; }
    public void setAmount(long amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
