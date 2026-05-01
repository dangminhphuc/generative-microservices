package com.fintech.transfer.domain.port.out;

import com.fintech.common.domain.Money;
import com.fintech.transfer.domain.model.Transfer;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {
    Transfer save(Transfer transfer);
    Optional<Transfer> findById(UUID id);
    Money sumAmountBySourceAccountAndDate(String sourceAccountNumber, LocalDate date);
}
