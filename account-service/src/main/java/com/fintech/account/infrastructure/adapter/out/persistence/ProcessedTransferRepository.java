package com.fintech.account.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedTransferRepository extends JpaRepository<ProcessedTransferJpaEntity, String> {
}
