package com.fintech.account.domain.port.in;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.common.dto.AccountInfoResponse;

public interface GetAccountByNumberUseCase {
    AccountInfoResponse execute(AccountNumber accountNumber);
}
