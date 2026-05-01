package com.fintech.history.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRecordResponse(
        String id,
        String transactionId,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyAccountNumber,
        String description,
        String status,
        Instant createdAt
) {}
