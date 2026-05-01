package com.fintech.history.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDetailResponse(
        String id,
        String transactionId,
        String accountNumber,
        String type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String counterpartyAccountNumber,
        String description,
        String status,
        Instant createdAt
) {}
