package com.fintech.history.application.service;

import com.fintech.common.exception.BusinessRuleException;
import com.fintech.common.exception.ResourceNotFoundException;
import com.fintech.history.application.dto.*;
import com.fintech.history.domain.model.TransactionRecord;
import com.fintech.history.domain.port.in.FilterTransactionsUseCase;
import com.fintech.history.domain.port.in.GetTransactionDetailUseCase;
import com.fintech.history.domain.port.in.GetTransactionHistoryUseCase;
import com.fintech.history.domain.port.out.TransactionRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TransactionHistoryService implements GetTransactionHistoryUseCase,
        FilterTransactionsUseCase, GetTransactionDetailUseCase {

    private static final int MAX_DATE_RANGE_MONTHS = 12;

    private final TransactionRecordRepository repository;

    public TransactionHistoryService(TransactionRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<TransactionRecordResponse> execute(String accountNumber, Pageable pageable) {
        return repository.findByAccountNumber(accountNumber, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<TransactionRecordResponse> execute(FilterTransactionsQuery query) {
        if (query.startDate() != null && query.endDate() != null) {
            if (query.startDate().isAfter(query.endDate())) {
                throw new BusinessRuleException("startDate phải trước endDate", "INVALID_DATE_RANGE");
            }
            long months = ChronoUnit.MONTHS.between(query.startDate(), query.endDate());
            if (months > MAX_DATE_RANGE_MONTHS) {
                throw new BusinessRuleException("Khoảng thời gian tối đa là 12 tháng", "DATE_RANGE_TOO_LARGE");
            }
        }

        var startInstant = query.startDate() != null
                ? query.startDate().atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        var endInstant = query.endDate() != null
                ? query.endDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        return repository.findByAccountNumberAndFilters(
                query.accountNumber(), startInstant, endInstant, query.type(), query.pageable()
        ).map(this::toResponse);
    }

    @Override
    public TransactionDetailResponse execute(UUID transactionRecordId) {
        TransactionRecord record = repository.findById(transactionRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionRecordId.toString()));

        return new TransactionDetailResponse(
                record.getId().toString(),
                record.getTransactionId(),
                record.getAccountNumber(),
                record.getType().name(),
                record.getAmount().getAmount(),
                record.getBalanceBefore().getAmount(),
                record.getBalanceAfter().getAmount(),
                record.getCounterpartyAccountNumber(),
                record.getDescription(),
                record.getStatus(),
                record.getCreatedAt()
        );
    }

    private TransactionRecordResponse toResponse(TransactionRecord record) {
        return new TransactionRecordResponse(
                record.getId().toString(),
                record.getTransactionId(),
                record.getType().name(),
                record.getAmount().getAmount(),
                record.getBalanceAfter().getAmount(),
                record.getCounterpartyAccountNumber(),
                record.getDescription(),
                record.getStatus(),
                record.getCreatedAt()
        );
    }
}
