package com.fintech.account.domain.port.in;

import com.fintech.account.application.dto.LoginResponse;
import com.fintech.account.application.dto.RefreshTokenCommand;

public interface RefreshTokenUseCase {
    LoginResponse execute(RefreshTokenCommand command);
}
