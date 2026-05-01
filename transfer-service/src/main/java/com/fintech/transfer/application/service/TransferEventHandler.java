package com.fintech.transfer.application.service;

import com.fintech.common.event.account.AccountCreditedEvent;
import com.fintech.common.event.account.DebitFailedEvent;
import com.fintech.common.event.transfer.TransferCompletedEvent;
import com.fintech.common.event.transfer.TransferFailedEvent;
import com.fintech.transfer.domain.model.Transfer;
import com.fintech.transfer.domain.port.out.EventPublisher;
import com.fintech.transfer.domain.port.out.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransferEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferEventHandler.class);

    private final TransferRepository transferRepository;
    private final EventPublisher eventPublisher;

    public TransferEventHandler(TransferRepository transferRepository, EventPublisher eventPublisher) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void handleAccountCredited(AccountCreditedEvent event) {
        String transferId = event.getTransferId();
        log.info("Handling AccountCreditedEvent for transfer {}", transferId);

        Transfer transfer = transferRepository.findById(UUID.fromString(transferId)).orElse(null);
        if (transfer == null || transfer.isTerminal()) {
            log.warn("Transfer {} not found or already terminal, skipping", transferId);
            return;
        }

        transfer.markCompleted();
        transferRepository.save(transfer);

        eventPublisher.publish(new TransferCompletedEvent(
                transferId,
                transfer.getSourceAccountNumber(),
                transfer.getDestinationAccountNumber(),
                transfer.getAmount().getAmount(),
                transfer.getAmount().getCurrency(),
                transfer.getDescription(),
                Instant.now()
        ));

        log.info("Transfer {} completed", transferId);
    }

    @Transactional
    public void handleDebitFailed(DebitFailedEvent event) {
        String transferId = event.getTransferId();
        log.info("Handling DebitFailedEvent for transfer {}", transferId);

        Transfer transfer = transferRepository.findById(UUID.fromString(transferId)).orElse(null);
        if (transfer == null || transfer.isTerminal()) {
            log.warn("Transfer {} not found or already terminal, skipping", transferId);
            return;
        }

        transfer.markFailed();
        transferRepository.save(transfer);

        eventPublisher.publish(new TransferFailedEvent(
                transferId,
                transfer.getSourceAccountNumber(),
                transfer.getDestinationAccountNumber(),
                transfer.getAmount().getAmount(),
                transfer.getAmount().getCurrency(),
                event.getReason(),
                Instant.now()
        ));

        log.info("Transfer {} failed: {}", transferId, event.getReason());
    }
}
