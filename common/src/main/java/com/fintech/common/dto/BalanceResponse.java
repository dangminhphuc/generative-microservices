package com.fintech.common.dto;

import java.math.BigDecimal;
import java.util.List;

public record BalanceResponse(List<AccountBalance> accounts) {

    public record AccountBalance(
            String accountId,
            String accountNumber,
            BigDecimal balance,
            String currency,
            String status
    ) {}
}
