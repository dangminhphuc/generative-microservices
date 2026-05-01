package com.fintech.account.domain.port.in;

import com.fintech.account.application.dto.RegisterUserCommand;
import com.fintech.account.application.dto.RegisterUserResponse;

public interface RegisterUserUseCase {
    RegisterUserResponse execute(RegisterUserCommand command);
}
