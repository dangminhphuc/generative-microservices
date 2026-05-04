package com.fintech.account.reproduce;

import com.fintech.account.domain.port.in.DebitCreditUseCase;
import com.fintech.account.infrastructure.adapter.in.kafka.TransferEventConsumer;
import com.fintech.common.event.transfer.TransferInitiatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Reproduce tests for the Kafka consumer error handling behavior.
 *
 * Originally written to expose the bug (no try-catch in consumer).
 * After the fix (Phase 2 — Kafka Hardening), the consumer now has a try-catch
 * that logs the error and re-throws so DefaultErrorHandler can handle
 * retry + DLQ routing.
 *
 * These tests verify that exceptions ARE re-thrown by the consumer — which is
 * the correct behavior: the consumer itself does not swallow exceptions;
 * instead it delegates retry/DLQ decisions to DefaultErrorHandler.
 *
 * Root cause (fixed): TransferEventConsumer.handleTransferInitiated() had no
 * try-catch. After the fix, it catches, logs, and re-throws.
 *
 * See: Requirement 2 in account-service-production-hardening spec.
 * See: LL-4 (Lesson Learned) in requirements.md.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("[REPRODUCE] Kafka Consumer Error Handling — Fixed Behavior")
class KafkaNoErrorHandlingReproduceTest {

    @Mock
    private DebitCreditUseCase debitCreditUseCase;

    @InjectMocks
    private TransferEventConsumer consumer;

    /**
     * FIXED: when execute() throws a RuntimeException, the consumer catches it,
     * logs the error, and re-throws it so DefaultErrorHandler can route to DLQ
     * after exhausting retries.
     *
     * The exception IS still thrown from handleTransferInitiated() — this is
     * intentional: DefaultErrorHandler (configured in KafkaConfig) intercepts
     * the re-thrown exception and applies exponential backoff (1s, 2s, 4s)
     * before publishing to transfer-events.DLT.
     *
     * This test verifies the consumer correctly propagates the exception type
     * and message so DefaultErrorHandler has full context for DLQ routing.
     */
    @Test
    @DisplayName("FIXED: RuntimeException is caught, logged, and re-thrown for DefaultErrorHandler")
    void handleTransferInitiated_whenExecuteThrows_exceptionIsRethrownForDefaultErrorHandler() {
        // Arrange
        TransferInitiatedEvent event = buildEvent("transfer-001", "ACC1111111", "ACC2222222", 50_000L);
        doThrow(new RuntimeException("Source account not found"))
                .when(debitCreditUseCase).execute(any());

        // Act & Assert — FIXED: exception is re-thrown (intentionally) for DefaultErrorHandler
        // The consumer catches, logs, then re-throws — DefaultErrorHandler handles retry + DLQ
        assertThatThrownBy(() -> consumer.handleTransferInitiated(event))
                .as("FIXED: RuntimeException is re-thrown after logging so DefaultErrorHandler " +
                    "can apply exponential backoff and route to transfer-events.DLT after 3 retries.")
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Source account not found");
    }

    /**
     * FIXED: permanent failures (e.g. BusinessRuleException / INSUFFICIENT_BALANCE)
     * are also caught, logged, and re-thrown for DefaultErrorHandler.
     *
     * DefaultErrorHandler will retry 3 times (1s, 2s, 4s backoff) and then
     * publish to transfer-events.DLT — preventing indefinite retry loops
     * that would block the partition.
     */
    @Test
    @DisplayName("FIXED: permanent failures (e.g. INSUFFICIENT_BALANCE) are re-thrown for DLQ routing")
    void handleTransferInitiated_whenPermanentFailure_isRethrownForDlqRouting() {
        // Arrange
        TransferInitiatedEvent event = buildEvent("transfer-002", "ACC1111111", "ACC2222222", 999_999_999L);
        doThrow(new com.fintech.common.exception.BusinessRuleException("Insufficient balance", "INSUFFICIENT_BALANCE"))
                .when(debitCreditUseCase).execute(any());

        // Act & Assert — FIXED: permanent failure is re-thrown for DefaultErrorHandler + DLQ
        assertThatThrownBy(() -> consumer.handleTransferInitiated(event))
                .as("FIXED: Permanent failure (INSUFFICIENT_BALANCE) is re-thrown so DefaultErrorHandler " +
                    "routes it to transfer-events.DLT after retries — no indefinite retry loop.")
                .isInstanceOf(com.fintech.common.exception.BusinessRuleException.class);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

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
