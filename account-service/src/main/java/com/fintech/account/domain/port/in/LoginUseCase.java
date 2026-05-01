package com.fintech.account.domain.port.in;

import com.fintech.account.application.dto.LoginCommand;
import com.fintech.account.application.dto.LoginResponse;

public interface LoginUseCase {
    LoginResponse execute(LoginCommand command);
}
