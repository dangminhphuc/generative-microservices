package com.fintech.account.reproduce;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.model.BankAccount;
import com.fintech.account.domain.port.out.BankAccountRepository;
import com.fintech.account.domain.port.out.EventPublisher;
import com.fintech.account.application.service.DebitCreditService;
import com.fintech.account.infrastructure.adapter.out.persistence.ProcessedTransferJpaEntity;
import com.fintech.account.infrastructure.adapter.out.persistence.ProcessedTransferRepository;
import com.fintech.common.domain.Money;
import com.fintech.common.event.transfer.TransferInitiatedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Reproduce tests for the missing idempotency in DebitCreditService.
 *
 * These tests were originally written to EXPOSE the bug (asserting buggy behavior).
 * After the fix (Phase 2 — Kafka Hardening), they have been updated to assert
 * CORRECT behavior: duplicate Kafka messages are ignored via the processed_transfers
 * idempotency table.
 *
 * Root cause (fixed): DebitCreditService had no idempotency check. Kafka guarantees
 * at-least-once delivery — the same TransferInitiatedEvent can be delivered
 * multiple times (consumer restart, rebalance, network retry). Without idempotency,
 * each duplicate caused another debit/credit — double-spending money.
 *
 * Fix: DebitCreditService now checks processedTransferRepository.existsById(transferId)
 * before processing. If already processed, it logs a warning and returns early.
 *
 * See: Requirement 3 in account-service-production-hardening spec.
 * See: LL-4 (Lesson Learned) in requirements.md.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("[REPRODUCE] DebitCreditService Idempotency — Fixed Behavior")
class KafkaNoIdempotencyReproduceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ProcessedTransferRepository processedTransferRepository;

    private DebitCreditService debitCreditService;

    @BeforeEach
    void setUp() {
        // Fixed DebitCreditService now requires ProcessedTransferRepository for idempotency
        debitCreditService = new DebitCreditService(
                bankAccountRepository,
                eventPublisher,
                processedTransferRepository,
                new SimpleMeterRegistry()
        );
    }

    /**
     * FIXED: calling execute() twice with the same transferId causes
     * bankAccountRepository.save() to be called exactly TWICE (once per account,
     * on the first execution only). The second call is a no-op.
     *
     * Previously (bug): save() was called 4 times — double debit confirmed.
     * Now (fixed): save() called 2 times — idempotency works correctly.
     */
    @Test
    @DisplayName("FIXED: duplicate Kafka message is ignored — save() called only twice (not four times)")
    void execute_withDuplicateTransferId_shouldBeIdempotent() {
        // Arrange — source has 200,000 VND, transfer amount is 50,000 VND
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        String transferId = "transfer-duplicate-001";

        BankAccount source = buildAccount(sourceId, "1111111111", 200_000L);
        BankAccount dest   = buildAccount(destId,   "2222222222", 0L);

        when(bankAccountRepository.findByAccountNumber(AccountNumber.of("1111111111")))
                .thenReturn(Optional.of(source));
        when(bankAccountRepository.findByAccountNumber(AccountNumber.of("2222222222")))
                .thenReturn(Optional.of(dest));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // First call: not yet processed
        when(processedTransferRepository.existsById(transferId)).thenReturn(false);
        when(processedTransferRepository.save(any(ProcessedTransferJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransferInitiatedEvent event = buildEvent(transferId, "1111111111", "2222222222", 50_000L);

        // Act — first execution processes normally
        debitCreditService.execute(event);

        // Second call: already processed — simulate idempotency table has the record
        when(processedTransferRepository.existsById(transferId)).thenReturn(true);

        debitCreditService.execute(event); // duplicate — should be ignored

        // Assert — FIXED: save() called exactly 2 times (source + dest, first execution only)
        verify(bankAccountRepository, times(2))
                .save(any(BankAccount.class));
    }

    /**
     * FIXED: after duplicate processing, the source account has been debited
     * exactly ONCE — balance is 150,000 VND (not 100,000 from double debit).
     *
     * Previously (bug): balance was 100,000 — debited twice.
     * Now (fixed): balance is 150,000 — debited exactly once.
     */
    @Test
    @DisplayName("FIXED: source account balance debited exactly once on duplicate message")
    void execute_withDuplicateTransferId_sourceBalanceDebitedOnce() {
        // Arrange — source has 200,000 VND, transfer 50,000 VND
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        String transferId = "transfer-duplicate-002";

        BankAccount[] sourceHolder = { buildAccount(sourceId, "3333333333", 200_000L) };
        BankAccount[] destHolder   = { buildAccount(destId,   "4444444444", 0L) };

        when(bankAccountRepository.findByAccountNumber(AccountNumber.of("3333333333")))
                .thenAnswer(inv -> Optional.of(sourceHolder[0]));
        when(bankAccountRepository.findByAccountNumber(AccountNumber.of("4444444444")))
                .thenAnswer(inv -> Optional.of(destHolder[0]));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processedTransferRepository.save(any(ProcessedTransferJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransferInitiatedEvent event = buildEvent(transferId, "3333333333", "4444444444", 50_000L);

        // First call: not yet processed
        when(processedTransferRepository.existsById(transferId)).thenReturn(false);
        debitCreditService.execute(event);

        // Second call: already processed
        when(processedTransferRepository.existsById(transferId)).thenReturn(true);
        debitCreditService.execute(event); // duplicate — should be ignored

        // Assert — FIXED: source debited exactly once → balance = 150,000 VND
        assertThat(sourceHolder[0].getBalance())
                .as("FIXED: source account debited exactly once. " +
                    "Balance should be 150,000 after one debit of 50,000 from 200,000.")
                .isEqualTo(Money.of(150_000L));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private BankAccount buildAccount(UUID id, String accountNumber, long balance) {
        BankAccount account = BankAccount.create(AccountNumber.of(accountNumber), UUID.randomUUID());
        if (balance > 0) {
            account.credit(Money.of(balance), "setup");
        }
        return account;
    }

    private TransferInitiatedEvent buildEvent(String transferId, String sourceAccount,
                                               String destAccount, long amount) {
        return new TransferInitiatedEvent(
                transferId,
                sourceAccount,
                destAccount,
                BigDecimal.valueOf(amount),
                "VND",
                "test transfer"
        );
    }
}
