package com.fintech.transfer.infrastructure.adapter.out.persistence;

import com.fintech.common.domain.Money;
import com.fintech.transfer.domain.model.Transfer;
import com.fintech.transfer.domain.port.out.TransferRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Component
public class TransferRepositoryAdapter implements TransferRepository {

    private final TransferJpaRepository jpaRepository;

    public TransferRepositoryAdapter(TransferJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transfer save(Transfer transfer) {
        TransferJpaEntity entity = toEntity(transfer);
        TransferJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Money sumAmountBySourceAccountAndDate(String sourceAccountNumber, LocalDate date) {
        var startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        var endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long total = jpaRepository.sumAmountBySourceAndDate(sourceAccountNumber, startOfDay, endOfDay);
        return Money.of(total);
    }

    private TransferJpaEntity toEntity(Transfer transfer) {
        TransferJpaEntity entity = new TransferJpaEntity();
        entity.setId(transfer.getId());
        entity.setSourceAccountNumber(transfer.getSourceAccountNumber());
        entity.setDestinationAccountNumber(transfer.getDestinationAccountNumber());
        entity.setAmount(transfer.getAmount().getAmount().longValue());
        entity.setCurrency(transfer.getAmount().getCurrency());
        entity.setDescription(transfer.getDescription());
        entity.setStatus(transfer.getStatus());
        entity.setUserId(transfer.getUserId());
        entity.setCreatedAt(transfer.getCreatedAt());
        entity.setCompletedAt(transfer.getCompletedAt());
        return entity;
    }

    private Transfer toDomain(TransferJpaEntity entity) {
        // Simplified reconstitution
        Transfer transfer = Transfer.create(
                entity.getSourceAccountNumber(),
                entity.getDestinationAccountNumber(),
                Money.of(entity.getAmount()),
                entity.getDescription(),
                entity.getUserId());
        if (entity.getStatus() == com.fintech.transfer.domain.model.TransferStatus.COMPLETED) {
            transfer.markCompleted();
        } else if (entity.getStatus() == com.fintech.transfer.domain.model.TransferStatus.FAILED) {
            transfer.markFailed();
        }
        return transfer;
    }
}
