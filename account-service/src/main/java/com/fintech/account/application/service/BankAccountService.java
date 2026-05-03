package com.fintech.account.application.service;

import com.fintech.account.application.dto.CreateBankAccountCommand;
import com.fintech.account.application.dto.CreateBankAccountResponse;
import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.domain.port.in.CreateBankAccountUseCase;
import com.fintech.account.domain.port.in.GetAccountByNumberUseCase;
import com.fintech.account.domain.port.in.GetBalanceUseCase;
import com.fintech.account.domain.port.out.BankAccountRepository;
import com.fintech.account.domain.port.out.EventPublisher;
import com.fintech.account.domain.port.out.UserRepository;
import com.fintech.account.domain.service.AccountNumberGenerator;
import com.fintech.common.dto.AccountInfoResponse;
import com.fintech.common.dto.BalanceResponse;
import com.fintech.common.event.account.AccountCreatedEvent;
import com.fintech.common.exception.BusinessRuleException;
import com.fintech.common.exception.ResourceNotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BankAccountService implements CreateBankAccountUseCase, GetBalanceUseCase, GetAccountByNumberUseCase {

    private static final Logger log = LoggerFactory.getLogger(BankAccountService.class);
    private static final int MAX_ACCOUNTS_PER_USER = 5;

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final AccountNumberGenerator accountNumberGenerator;
    private final Counter accountCreatedCounter;

    public BankAccountService(BankAccountRepository bankAccountRepository,
                               UserRepository userRepository,
                               EventPublisher eventPublisher,
                               AccountNumberGenerator accountNumberGenerator,
                               MeterRegistry meterRegistry) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.accountNumberGenerator = accountNumberGenerator;
        this.accountCreatedCounter = Counter.builder("account.created").register(meterRegistry);
    }

    @Override
    @Transactional
    @CacheEvict(value = "accounts-by-userId", key = "#command.userId()")
    public CreateBankAccountResponse execute(CreateBankAccountCommand command) {
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", command.userId().toString()));

        int count = bankAccountRepository.countByUserId(command.userId());
        if (count >= MAX_ACCOUNTS_PER_USER) {
            throw new BusinessRuleException("Đã đạt giới hạn số tài khoản", "MAX_ACCOUNTS_REACHED");
        }

        AccountNumber accountNumber = accountNumberGenerator.generate();
        BankAccount account = BankAccount.create(accountNumber, command.userId());
        BankAccount saved = bankAccountRepository.save(account);

        log.info("Bank account created accountId={} userId={}", saved.getId(), command.userId());
        accountCreatedCounter.increment();

        eventPublisher.publish(new AccountCreatedEvent(
                saved.getId().toString(),
                saved.getAccountNumber().getValue(),
                command.userId().toString()
        ));

        return new CreateBankAccountResponse(
                saved.getId().toString(),
                saved.getAccountNumber().getValue(),
                saved.getBalance().getAmount(),
                saved.getStatus().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts-by-userId", key = "#userId")
    public BalanceResponse execute(UUID userId) {
        var accounts = bankAccountRepository.findByUserId(userId);
        var balances = accounts.stream()
                .map(a -> new BalanceResponse.AccountBalance(
                        a.getId().toString(),
                        a.getAccountNumber().getValue(),
                        a.getBalance().getAmount(),
                        a.getBalance().getCurrency(),
                        a.getStatus().name()
                ))
                .toList();
        return new BalanceResponse(balances);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts-by-number", key = "#accountNumber.value")
    public AccountInfoResponse execute(AccountNumber accountNumber) {
        var account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber.getValue()));
        var user = userRepository.findById(account.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", account.getUserId().toString()));

        return new AccountInfoResponse(
                account.getId().toString(),
                account.getAccountNumber().getValue(),
                user.getFullName(),
                account.getStatus().name()
        );
    }
}
