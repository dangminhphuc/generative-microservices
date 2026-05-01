package com.fintech.history.infrastructure.adapter.in.kafka;

import com.fintech.common.event.transfer.TransferCompletedEvent;
import com.fintech.common.event.transfer.TransferFailedEvent;
import com.fintech.history.application.service.TransferEventRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransferEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferEventConsumer.class);

    private final TransferEventRecorder transferEventRecorder;

    public TransferEventConsumer(TransferEventRecorder transferEventRecorder) {
        this.transferEventRecorder = transferEventRecorder;
    }

    @KafkaListener(topics = "transfer-events", groupId = "history-service-group")
    public void handleTransferEvent(Object event) {
        if (event instanceof TransferCompletedEvent completedEvent) {
            log.info("Received TransferCompletedEvent for transfer {}", completedEvent.getAggregateId());
            transferEventRecorder.handleTransferCompleted(completedEvent);
        } else if (event instanceof TransferFailedEvent failedEvent) {
            log.info("Received TransferFailedEvent for transfer {}", failedEvent.getAggregateId());
            transferEventRecorder.handleTransferFailed(failedEvent);
        }
    }
}
