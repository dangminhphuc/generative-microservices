package com.fintech.account.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountJpaRepository extends JpaRepository<BankAccountJpaEntity, UUID> {
    Optional<BankAccountJpaEntity> findByAccountNumber(String accountNumber);
    List<BankAccountJpaEntity> findByUserId(UUID userId);
    int countByUserId(UUID userId);
    boolean existsByAccountNumber(String accountNumber);
}
