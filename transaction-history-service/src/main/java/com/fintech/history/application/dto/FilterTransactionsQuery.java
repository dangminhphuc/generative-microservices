package com.fintech.history.application.dto;

import com.fintech.history.domain.model.TransactionType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public record FilterTransactionsQuery(
        String accountNumber,
        LocalDate startDate,
        LocalDate endDate,
        TransactionType type,
        Pageable pageable
) {}
