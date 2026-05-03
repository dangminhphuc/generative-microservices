package com.fintech.account.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "processed_transfers")
public class ProcessedTransferJpaEntity {

    @Id
    @Column(name = "transfer_id", length = 36)
    private String transferId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedTransferJpaEntity() {}

    public ProcessedTransferJpaEntity(String transferId, Instant processedAt) {
        this.transferId = transferId;
        this.processedAt = processedAt;
    }

    public String getTransferId() { return transferId; }
    public Instant getProcessedAt() { return processedAt; }
}
