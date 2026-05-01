package com.fintech.history.infrastructure.adapter.out.persistence;

import com.fintech.common.domain.Money;
import com.fintech.history.domain.model.TransactionRecord;
import com.fintech.history.domain.model.TransactionType;
import com.fintech.history.domain.port.out.TransactionRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class TransactionRecordRepositoryAdapter implements TransactionRecordRepository {

    private final TransactionRecordJpaRepository jpaRepository;

    public TransactionRecordRepositoryAdapter(TransactionRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TransactionRecord save(TransactionRecord record) {
        TransactionRecordJpaEntity entity = toEntity(record);
        jpaRepository.save(entity);
        return record;
    }

    @Override
    public Optional<TransactionRecord> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByTransactionIdAndAccountNumber(String transactionId, String accountNumber) {
        return jpaRepository.existsByTransactionIdAndAccountNumber(transactionId, accountNumber);
    }

    @Override
    public Page<TransactionRecord> findByAccountNumber(String accountNumber, Pageable pageable) {
        return jpaRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber, pageable)
                .map(this::toDomain);
    }

    @Override
    public Page<TransactionRecord> findByAccountNumberAndFilters(String accountNumber,
                                                                  Instant startDate, Instant endDate,
                                                                  TransactionType type, Pageable pageable) {
        return jpaRepository.findByFilters(accountNumber, startDate, endDate, type, pageable)
                .map(this::toDomain);
    }

    private TransactionRecordJpaEntity toEntity(TransactionRecord record) {
        TransactionRecordJpaEntity entity = new TransactionRecordJpaEntity();
        entity.setId(record.getId());
        entity.setTransactionId(record.getTransactionId());
        entity.setAccountNumber(record.getAccountNumber());
        entity.setType(record.getType());
        entity.setAmount(record.getAmount().getAmount().longValue());
        entity.setCurrency(record.getAmount().getCurrency());
        entity.setBalanceBefore(record.getBalanceBefore().getAmount().longValue());
        entity.setBalanceAfter(record.getBalanceAfter().getAmount().longValue());
        entity.setCounterpartyAccountNumber(record.getCounterpartyAccountNumber());
        entity.setDescription(record.getDescription());
        entity.setStatus(record.getStatus());
        entity.setCreatedAt(record.getCreatedAt());
        return entity;
    }

    private TransactionRecord toDomain(TransactionRecordJpaEntity entity) {
        return TransactionRecord.create(
                entity.getTransactionId(),
                entity.getAccountNumber(),
                entity.getType(),
                Money.of(entity.getAmount()),
                Money.of(entity.getBalanceBefore()),
                Money.of(entity.getBalanceAfter()),
                entity.getCounterpartyAccountNumber(),
                entity.getDescription(),
                entity.getStatus()
        );
    }
}
