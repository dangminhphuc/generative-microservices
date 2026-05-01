package com.fintech.history.application.service;

import com.fintech.common.domain.Money;
import com.fintech.common.event.transfer.TransferCompletedEvent;
import com.fintech.common.event.transfer.TransferFailedEvent;
import com.fintech.history.domain.model.TransactionRecord;
import com.fintech.history.domain.model.TransactionType;
import com.fintech.history.domain.port.out.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferEventRecorder {

    private static final Logger log = LoggerFactory.getLogger(TransferEventRecorder.class);

    private final TransactionRecordRepository repository;

    public TransferEventRecorder(TransactionRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void handleTransferCompleted(TransferCompletedEvent event) {
        String transferId = event.getAggregateId();
        Money amount = Money.of(event.getAmount(), event.getCurrency());

        // Idempotency check
        if (repository.existsByTransactionIdAndAccountNumber(transferId, event.getSourceAccountNumber())) {
            log.info("Transfer {} already recorded for source, skipping", transferId);
            return;
        }

        // DEBIT record for source account
        TransactionRecord debitRecord = TransactionRecord.create(
                transferId,
                event.getSourceAccountNumber(),
                TransactionType.DEBIT,
                amount,
                Money.zero(), // balanceBefore — not available from event, placeholder
                Money.zero(), // balanceAfter — not available from event, placeholder
                event.getDestinationAccountNumber(),
                event.getDescription(),
                "COMPLETED"
        );
        repository.save(debitRecord);

        // CREDIT record for destination account
        TransactionRecord creditRecord = TransactionRecord.create(
                transferId,
                event.getDestinationAccountNumber(),
                TransactionType.CREDIT,
                amount,
                Money.zero(),
                Money.zero(),
                event.getSourceAccountNumber(),
                event.getDescription(),
                "COMPLETED"
        );
        repository.save(creditRecord);

        log.info("Recorded transfer {} — DEBIT {} CREDIT {}", transferId,
                event.getSourceAccountNumber(), event.getDestinationAccountNumber());
    }

    @Transactional
    public void handleTransferFailed(TransferFailedEvent event) {
        String transferId = event.getAggregateId();

        if (repository.existsByTransactionIdAndAccountNumber(transferId, event.getSourceAccountNumber())) {
            log.info("Transfer {} already recorded, skipping", transferId);
            return;
        }

        TransactionRecord failedRecord = TransactionRecord.create(
                transferId,
                event.getSourceAccountNumber(),
                TransactionType.DEBIT,
                Money.of(event.getAmount(), event.getCurrency()),
                Money.zero(),
                Money.zero(),
                event.getDestinationAccountNumber(),
                event.getReason(),
                "FAILED"
        );
        repository.save(failedRecord);

        log.info("Recorded failed transfer {}: {}", transferId, event.getReason());
    }
}
