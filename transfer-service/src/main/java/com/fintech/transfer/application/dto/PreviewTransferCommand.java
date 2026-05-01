package com.fintech.transfer.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record PreviewTransferCommand(
        @NotNull UUID userId,
        @NotBlank @Pattern(regexp = "^\\d{10}$") String sourceAccountNumber,
        @NotBlank @Pattern(regexp = "^\\d{10}$") String destinationAccountNumber,
        @NotNull @Positive BigDecimal amount,
        String description
) {}
