package com.fintech.account.domain.port.in;

import com.fintech.account.application.dto.CreateBankAccountCommand;
import com.fintech.account.application.dto.CreateBankAccountResponse;

public interface CreateBankAccountUseCase {
    CreateBankAccountResponse execute(CreateBankAccountCommand command);
}
