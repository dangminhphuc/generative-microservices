package com.fintech.history.domain.port.out;

import com.fintech.history.domain.model.TransactionRecord;
import com.fintech.history.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRecordRepository {
    TransactionRecord save(TransactionRecord record);
    Optional<TransactionRecord> findById(UUID id);
    boolean existsByTransactionIdAndAccountNumber(String transactionId, String accountNumber);
    Page<TransactionRecord> findByAccountNumber(String accountNumber, Pageable pageable);
    Page<TransactionRecord> findByAccountNumberAndFilters(String accountNumber,
                                                          Instant startDate, Instant endDate,
                                                          TransactionType type, Pageable pageable);
}
