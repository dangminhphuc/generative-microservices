package com.fintech.account.application.service;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.domain.port.in.DebitCreditUseCase;
import com.fintech.account.domain.port.out.BankAccountRepository;
import com.fintech.account.domain.port.out.EventPublisher;
import com.fintech.account.infrastructure.adapter.out.persistence.ProcessedTransferJpaEntity;
import com.fintech.account.infrastructure.adapter.out.persistence.ProcessedTransferRepository;
import com.fintech.common.domain.Money;
import com.fintech.common.event.account.AccountCreditedEvent;
import com.fintech.common.event.account.AccountDebitedEvent;
import com.fintech.common.event.account.DebitFailedEvent;
import com.fintech.common.event.transfer.TransferInitiatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DebitCreditService implements DebitCreditUseCase {

    private static final Logger log = LoggerFactory.getLogger(DebitCreditService.class);

    private final BankAccountRepository bankAccountRepository;
    private final EventPublisher eventPublisher;
    private final ProcessedTransferRepository processedTransferRepository;
    private final Counter debitSuccessCounter;
    private final Counter debitFailureCounter;

    public DebitCreditService(BankAccountRepository bankAccountRepository, EventPublisher eventPublisher,
                              ProcessedTransferRepository processedTransferRepository,
                              MeterRegistry meterRegistry) {
        this.bankAccountRepository = bankAccountRepository;
        this.eventPublisher = eventPublisher;
        this.processedTransferRepository = processedTransferRepository;
        this.debitSuccessCounter = Counter.builder("account.debit.success").register(meterRegistry);
        this.debitFailureCounter = Counter.builder("account.debit.failure").register(meterRegistry);
    }

    @Override
    @Transactional
    public void execute(TransferInitiatedEvent event) {
        String transferId = event.getAggregateId();

        // Idempotency check
        if (processedTransferRepository.existsById(transferId)) {
            log.warn("Duplicate transfer event ignored: {}", transferId);
            return;
        }

        AccountNumber sourceNumber = AccountNumber.of(event.getSourceAccountNumber());
        AccountNumber destNumber = AccountNumber.of(event.getDestinationAccountNumber());
        Money amount = Money.of(event.getAmount(), event.getCurrency());

        try {
            BankAccount source = bankAccountRepository.findByAccountNumber(sourceNumber)
                    .orElseThrow(() -> new RuntimeException("Source account not found"));

            source.debit(amount, transferId);
            bankAccountRepository.save(source);

            eventPublisher.publish(new AccountDebitedEvent(
                    source.getId().toString(),
                    source.getAccountNumber().getValue(),
                    amount.getAmount(), amount.getCurrency(), transferId
            ));

            BankAccount dest = bankAccountRepository.findByAccountNumber(destNumber)
                    .orElseThrow(() -> new RuntimeException("Destination account not found"));

            dest.credit(amount, transferId);
            bankAccountRepository.save(dest);

            eventPublisher.publish(new AccountCreditedEvent(
                    dest.getId().toString(),
                    dest.getAccountNumber().getValue(),
                    amount.getAmount(), amount.getCurrency(), transferId
            ));

            log.info("Transfer {} processed: debit {} credit {}", transferId, sourceNumber, destNumber);
            debitSuccessCounter.increment();

            // Mark as processed within the same transaction
            processedTransferRepository.save(new ProcessedTransferJpaEntity(transferId, Instant.now()));

        } catch (Exception e) {
            log.error("Transfer {} failed: {}", transferId, e.getMessage());
            debitFailureCounter.increment();
            eventPublisher.publish(new DebitFailedEvent(
                    transferId,
                    event.getSourceAccountNumber(),
                    event.getAmount(), event.getCurrency(),
                    transferId, e.getMessage()
            ));
        }
    }
}
