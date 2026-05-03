package com.fintech.account.infrastructure.adapter.in.kafka;

import com.fintech.account.domain.port.in.DebitCreditUseCase;
import com.fintech.common.event.transfer.TransferInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransferEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferEventConsumer.class);

    private final DebitCreditUseCase debitCreditUseCase;

    public TransferEventConsumer(DebitCreditUseCase debitCreditUseCase) {
        this.debitCreditUseCase = debitCreditUseCase;
    }

    @KafkaListener(topics = "transfer-events", groupId = "account-service-group")
    public void handleTransferInitiated(TransferInitiatedEvent event) {
        log.info("Received TransferInitiatedEvent for transfer {}", event.getAggregateId());
        try {
            debitCreditUseCase.execute(event);
        } catch (Exception e) {
            log.error("Failed to process transfer {}: {}", event.getAggregateId(), e.getMessage());
            throw e;  // Re-throw để DefaultErrorHandler xử lý retry + DLQ
        }
    }
}
