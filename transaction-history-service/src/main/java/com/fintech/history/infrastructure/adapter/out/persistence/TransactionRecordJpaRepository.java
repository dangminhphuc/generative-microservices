package com.fintech.history.infrastructure.adapter.out.persistence;

import com.fintech.history.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface TransactionRecordJpaRepository extends JpaRepository<TransactionRecordJpaEntity, UUID> {

    Page<TransactionRecordJpaEntity> findByAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);

    boolean existsByTransactionIdAndAccountNumber(String transactionId, String accountNumber);

    @Query("SELECT t FROM TransactionRecordJpaEntity t WHERE t.accountNumber = :account " +
           "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.createdAt < :endDate) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "ORDER BY t.createdAt DESC")
    Page<TransactionRecordJpaEntity> findByFilters(@Param("account") String accountNumber,
                                                    @Param("startDate") Instant startDate,
                                                    @Param("endDate") Instant endDate,
                                                    @Param("type") TransactionType type,
                                                    Pageable pageable);
}
