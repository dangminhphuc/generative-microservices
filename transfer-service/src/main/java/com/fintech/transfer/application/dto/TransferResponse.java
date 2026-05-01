package com.fintech.transfer.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        String transferId, String status, BigDecimal amount, Instant createdAt) {}
