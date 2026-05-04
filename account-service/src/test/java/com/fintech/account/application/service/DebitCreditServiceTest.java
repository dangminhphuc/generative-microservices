package com.fintech.account.application.service;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.AccountStatus;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.domain.port.out.BankAccountRepository;
import com.fintech.account.domain.port.out.EventPublisher;
import com.fintech.account.infrastructure.adapter.out.persistence.ProcessedTransferJpaEntity;
import com.fintech.account.infrastructure.adapter.out.persistence.ProcessedTransferRepository;
import com.fintech.common.domain.Money;
import com.fintech.common.event.BaseDomainEvent;
import com.fintech.common.event.account.DebitFailedEvent;
import com.fintech.common.event.transfer.TransferInitiatedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitCreditServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ProcessedTransferRepository processedTransferRepository;

    private DebitCreditService debitCreditService;

    private static final String SOURCE_ACCOUNT_NUMBER = "1111111111";
    private static final String DEST_ACCOUNT_NUMBER   = "2222222222";
    private static final String TRANSFER_ID           = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        debitCreditService = new DebitCreditService(
                bankAccountRepository,
                eventPublisher,
                processedTransferRepository,
                new SimpleMeterRegistry()
        );
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private BankAccount accountWithBalance(String accountNumber, long balanceVnd) {
        BankAccount account = BankAccount.reconstitute(
                UUID.randomUUID(),
                AccountNumber.of(accountNumber),
                UUID.randomUUID(),
                Money.of(balanceVnd),
                AccountStatus.ACTIVE,
                0L,
                Instant.now(),
                Instant.now()
        );
        return account;
    }

    private TransferInitiatedEvent transferEvent(String transferId, long amountVnd) {
        return new TransferInitiatedEvent(
                transferId,
                SOURCE_ACCOUNT_NUMBER,
                DEST_ACCOUNT_NUMBER,
                BigDecimal.valueOf(amountVnd),
                "VND",
                "test transfer"
        );
    }

    // ─── tests ────────────────────────────────────────────────────────────────

    @Test
    void execute_withSufficientBalance_debitsSourceAndCreditsDestination() {
        // Arrange
        BankAccount source = accountWithBalance(SOURCE_ACCOUNT_NUMBER, 100_000L);
        BankAccount dest   = accountWithBalance(DEST_ACCOUNT_NUMBER, 0L);

        when(processedTransferRepository.existsById(TRANSFER_ID)).thenReturn(false);
        when(bankAccountRepository.findByAccountNumber(AccountNumber.of(SOURCE_ACCOUNT_NUMBER)))
                .thenReturn(Optional.of(source));
        when(bankAccountRepository.findByAccountNumber(AccountNumber.of(DEST_ACCOUNT_NUMBER)))
                .thenReturn(Optional.of(dest));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferInitiatedEvent event = transferEvent(TRANSFER_ID, 50_000L);

        // Act
        debitCreditService.execute(event);

        // Assert — balances updated
        assertThat(source.getBalance()).isEqualTo(Money.of(50_000L));
        assertThat(dest.getBalance()).isEqualTo(Money.of(50_000L));

        // Assert — publish called exactly 2 times (AccountDebitedEvent + AccountCreditedEvent)
        verify(eventPublisher, times(2)).publish(any(BaseDomainEvent.class));
    }

    @Test
    void execute_withInsufficientBalance_publishesDebitFailedEvent() {
        // Arrange — source has only 10k, trying to debit 50k
        BankAccount source = accountWithBalance(SOURCE_ACCOUNT_NUMBER, 10_000L);

        when(processedTransferRepository.existsById(TRANSFER_ID)).thenReturn(false);
        when(bankAccountRepository.findByAccountNumber(AccountNumber.of(SOURCE_ACCOUNT_NUMBER)))
                .thenReturn(Optional.of(source));

        TransferInitiatedEvent event = transferEvent(TRANSFER_ID, 50_000L);

        // Act
        debitCreditService.execute(event);

        // Assert — DebitFailedEvent was published
        ArgumentCaptor<BaseDomainEvent> captor = ArgumentCaptor.forClass(BaseDomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DebitFailedEvent.class);
    }

    @Test
    void execute_withInsufficientBalance_doesNotCreditDestination() {
        // Arrange — debit will fail, so destination save() must never be called
        BankAccount source = accountWithBalance(SOURCE_ACCOUNT_NUMBER, 10_000L);

        when(processedTransferRepository.existsById(TRANSFER_ID)).thenReturn(false);
        when(bankAccountRepository.findByAccountNumber(AccountNumber.of(SOURCE_ACCOUNT_NUMBER)))
                .thenReturn(Optional.of(source));

        TransferInitiatedEvent event = transferEvent(TRANSFER_ID, 50_000L);

        // Act
        debitCreditService.execute(event);

        // Assert — destination account was never looked up or saved
        verify(bankAccountRepository, never())
                .findByAccountNumber(AccountNumber.of(DEST_ACCOUNT_NUMBER));
        // save() may be called 0 times (source debit throws before save) or not at all
        verify(bankAccountRepository, never()).save(argThat(
                acct -> acct.getAccountNumber().getValue().equals(DEST_ACCOUNT_NUMBER)
        ));
    }

    @Test
    void execute_withDuplicateTransferId_isIdempotent() {
        // Arrange — transfer already processed
        when(processedTransferRepository.existsById(TRANSFER_ID)).thenReturn(true);

        TransferInitiatedEvent event = transferEvent(TRANSFER_ID, 50_000L);

        // Act
        debitCreditService.execute(event);

        // Assert — no account lookups or saves
        verify(bankAccountRepository, never()).findByAccountNumber(any());
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    void execute_onSuccess_savesTransferId() {
        // Arrange
        BankAccount source = accountWithBalance(SOURCE_ACCOUNT_NUMBER, 100_000L);
        BankAccount dest   = accountWithBalance(DEST_ACCOUNT_NUMBER, 0L);

        when(processedTransferRepository.existsById(TRANSFER_ID)).thenReturn(false);
        when(bankAccountRepository.findByAccountNumber(AccountNumber.of(SOURCE_ACCOUNT_NUMBER)))
                .thenReturn(Optional.of(source));
        when(bankAccountRepository.findByAccountNumber(AccountNumber.of(DEST_ACCOUNT_NUMBER)))
                .thenReturn(Optional.of(dest));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferInitiatedEvent event = transferEvent(TRANSFER_ID, 50_000L);

        // Act
        debitCreditService.execute(event);

        // Assert — processedTransferRepository.save() called with correct transferId
        ArgumentCaptor<ProcessedTransferJpaEntity> captor =
                ArgumentCaptor.forClass(ProcessedTransferJpaEntity.class);
        verify(processedTransferRepository).save(captor.capture());
        assertThat(captor.getValue().getTransferId()).isEqualTo(TRANSFER_ID);
    }
}
