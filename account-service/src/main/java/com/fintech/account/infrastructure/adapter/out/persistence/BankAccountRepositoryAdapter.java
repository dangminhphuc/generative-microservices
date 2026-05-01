package com.fintech.account.infrastructure.adapter.out.persistence;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.domain.port.out.BankAccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BankAccountRepositoryAdapter implements BankAccountRepository {

    private final BankAccountJpaRepository jpaRepository;

    public BankAccountRepositoryAdapter(BankAccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BankAccount save(BankAccount account) {
        BankAccountJpaEntity entity = BankAccountJpaEntity.fromDomain(account);
        BankAccountJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<BankAccount> findById(UUID id) {
        return jpaRepository.findById(id).map(BankAccountJpaEntity::toDomain);
    }

    @Override
    public Optional<BankAccount> findByAccountNumber(AccountNumber accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber.getValue()).map(BankAccountJpaEntity::toDomain);
    }

    @Override
    public List<BankAccount> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(BankAccountJpaEntity::toDomain)
                .toList();
    }

    @Override
    public int countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public boolean existsByAccountNumber(AccountNumber accountNumber) {
        return jpaRepository.existsByAccountNumber(accountNumber.getValue());
    }
}
