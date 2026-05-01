package com.fintech.transfer.domain.port.out;

import com.fintech.common.dto.AccountInfoResponse;

public interface AccountServicePort {
    AccountInfoResponse getAccountByNumber(String accountNumber);
}
