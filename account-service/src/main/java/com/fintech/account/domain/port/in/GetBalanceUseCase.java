package com.fintech.account.domain.port.in;

import com.fintech.common.dto.BalanceResponse;
import java.util.UUID;

public interface GetBalanceUseCase {
    BalanceResponse execute(UUID userId);
}
