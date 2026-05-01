package com.fintech.transfer.application.dto;

import java.math.BigDecimal;

public record TransferPreviewResponse(
        String sourceAccountName,
        String destinationAccountName,
        BigDecimal amount,
        String description
) {}
