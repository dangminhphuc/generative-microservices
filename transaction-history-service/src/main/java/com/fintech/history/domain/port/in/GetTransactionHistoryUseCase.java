package com.fintech.history.domain.port.in;

import com.fintech.history.application.dto.TransactionRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetTransactionHistoryUseCase {
    Page<TransactionRecordResponse> execute(String accountNumber, Pageable pageable);
}
