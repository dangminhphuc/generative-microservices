package com.fintech.account.application.dto;

import java.math.BigDecimal;

public record CreateBankAccountResponse(String accountId,
                                        String accountNumber,
                                        BigDecimal balance,
                                        String status) { }