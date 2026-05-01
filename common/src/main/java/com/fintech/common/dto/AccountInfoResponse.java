package com.fintech.common.dto;

public record AccountInfoResponse(
        String accountId,
        String accountNumber,
        String ownerName,
        String status
) {}
