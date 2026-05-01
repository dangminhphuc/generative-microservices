package com.fintech.history.domain.port.in;

import com.fintech.history.application.dto.FilterTransactionsQuery;
import com.fintech.history.application.dto.TransactionRecordResponse;
import org.springframework.data.domain.Page;

public interface FilterTransactionsUseCase {
    Page<TransactionRecordResponse> execute(FilterTransactionsQuery query);
}
