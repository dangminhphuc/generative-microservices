package com.fintech.history.domain.port.in;

import com.fintech.history.application.dto.TransactionDetailResponse;
import java.util.UUID;

public interface GetTransactionDetailUseCase {
    TransactionDetailResponse execute(UUID transactionRecordId);
}
