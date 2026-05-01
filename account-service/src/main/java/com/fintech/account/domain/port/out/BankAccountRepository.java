package com.fintech.account.domain.port.out;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.BankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository {
    BankAccount save(BankAccount account);
    Optional<BankAccount> findById(UUID id);
    Optional<BankAccount> findByAccountNumber(AccountNumber accountNumber);
    List<BankAccount> findByUserId(UUID userId);
    int countByUserId(UUID userId);
    boolean existsByAccountNumber(AccountNumber accountNumber);
}
