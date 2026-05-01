package com.fintech.transfer.domain.port.in;

import com.fintech.transfer.application.dto.InitiateTransferCommand;
import com.fintech.transfer.application.dto.TransferResponse;

public interface InitiateTransferUseCase {
    TransferResponse execute(InitiateTransferCommand command);
}
