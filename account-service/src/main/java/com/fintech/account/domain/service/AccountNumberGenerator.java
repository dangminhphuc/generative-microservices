package com.fintech.account.domain.service;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.port.out.BankAccountRepository;

import java.security.SecureRandom;

public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 3;

    private final BankAccountRepository repository;

    public AccountNumberGenerator(BankAccountRepository repository) {
        this.repository = repository;
    }

    public AccountNumber generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String number = String.format("%010d", RANDOM.nextLong(1_000_000_000L, 10_000_000_000L));
            AccountNumber accountNumber = AccountNumber.of(number);
            if (!repository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }
        throw new IllegalStateException("Failed to generate unique account number after " + MAX_ATTEMPTS + " attempts");
    }
}
