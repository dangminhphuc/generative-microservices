package com.fintech.history.infrastructure.adapter.in.rest;

import com.fintech.history.application.dto.*;
import com.fintech.history.domain.model.TransactionType;
import com.fintech.history.domain.port.in.FilterTransactionsUseCase;
import com.fintech.history.domain.port.in.GetTransactionDetailUseCase;
import com.fintech.history.domain.port.in.GetTransactionHistoryUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionHistoryController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final GetTransactionHistoryUseCase getHistoryUseCase;
    private final FilterTransactionsUseCase filterUseCase;
    private final GetTransactionDetailUseCase getDetailUseCase;

    public TransactionHistoryController(GetTransactionHistoryUseCase getHistoryUseCase,
                                         FilterTransactionsUseCase filterUseCase,
                                         GetTransactionDetailUseCase getDetailUseCase) {
        this.getHistoryUseCase = getHistoryUseCase;
        this.filterUseCase = filterUseCase;
        this.getDetailUseCase = getDetailUseCase;
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Page<TransactionRecordResponse>> getHistory(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, DEFAULT_PAGE_SIZE));
        Page<TransactionRecordResponse> result = getHistoryUseCase.execute(accountNumber, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/account/{accountNumber}/filter")
    public ResponseEntity<Page<TransactionRecordResponse>> filterTransactions(
            @PathVariable String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new FilterTransactionsQuery(
                accountNumber, startDate, endDate, type,
                PageRequest.of(page, Math.min(size, DEFAULT_PAGE_SIZE)));
        Page<TransactionRecordResponse> result = filterUseCase.execute(query);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{transactionRecordId}")
    public ResponseEntity<TransactionDetailResponse> getDetail(
            @PathVariable UUID transactionRecordId) {
        TransactionDetailResponse result = getDetailUseCase.execute(transactionRecordId);
        return ResponseEntity.ok(result);
    }
}
