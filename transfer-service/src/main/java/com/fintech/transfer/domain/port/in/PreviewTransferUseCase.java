package com.fintech.transfer.domain.port.in;

import com.fintech.transfer.application.dto.PreviewTransferCommand;
import com.fintech.transfer.application.dto.TransferPreviewResponse;

public interface PreviewTransferUseCase {
    TransferPreviewResponse execute(PreviewTransferCommand command);
}
