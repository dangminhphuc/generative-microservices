package com.fintech.transfer.infrastructure.adapter.in.kafka;

import com.fintech.common.event.account.AccountCreditedEvent;
import com.fintech.common.event.account.DebitFailedEvent;
import com.fintech.transfer.application.service.TransferEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);

    private final TransferEventHandler transferEventHandler;

    public AccountEventConsumer(TransferEventHandler transferEventHandler) {
        this.transferEventHandler = transferEventHandler;
    }

    @KafkaListener(topics = "account-events", groupId = "transfer-service-group")
    public void handleAccountEvent(Object event) {
        if (event instanceof AccountCreditedEvent creditedEvent) {
            log.info("Received AccountCreditedEvent for transfer {}", creditedEvent.getTransferId());
            transferEventHandler.handleAccountCredited(creditedEvent);
        } else if (event instanceof DebitFailedEvent failedEvent) {
            log.info("Received DebitFailedEvent for transfer {}", failedEvent.getTransferId());
            transferEventHandler.handleDebitFailed(failedEvent);
        }
    }
}
