package com.fintech.account.domain.port.in;

import com.fintech.common.event.transfer.TransferInitiatedEvent;

public interface DebitCreditUseCase {
    void execute(TransferInitiatedEvent event);
}
