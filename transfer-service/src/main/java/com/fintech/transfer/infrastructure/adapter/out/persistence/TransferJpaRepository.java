package com.fintech.transfer.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransferJpaEntity t " +
           "WHERE t.sourceAccountNumber = :source " +
           "AND t.status IN ('PENDING', 'COMPLETED') " +
           "AND t.createdAt >= :startOfDay AND t.createdAt < :endOfDay")
    long sumAmountBySourceAndDate(@Param("source") String sourceAccountNumber,
                                  @Param("startOfDay") Instant startOfDay,
                                  @Param("endOfDay") Instant endOfDay);
}
