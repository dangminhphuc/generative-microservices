package com.fintech.transfer.domain.service;

import com.fintech.common.domain.Money;
import com.fintech.common.exception.BusinessRuleException;

public class TransferValidationService {

    private final long minimumAmount;
    private final long dailyLimit;

    public TransferValidationService(long minimumAmount, long dailyLimit) {
        this.minimumAmount = minimumAmount;
        this.dailyLimit = dailyLimit;
    }

    public void validateMinimumAmount(Money amount) {
        if (amount.isLessThan(Money.of(minimumAmount))) {
            throw new BusinessRuleException(
                    "Số tiền chuyển phải >= %,d VND".formatted(minimumAmount), "MINIMUM_AMOUNT");
        }
    }

    public void validateNotSameAccount(String source, String destination) {
        if (source.equals(destination)) {
            throw new BusinessRuleException(
                    "Không thể chuyển tiền cho chính mình", "SAME_ACCOUNT");
        }
    }

    public void validateDailyLimit(Money todayTotal, Money newAmount) {
        Money total = todayTotal.add(newAmount);
        if (total.isGreaterThan(Money.of(dailyLimit))) {
            throw new BusinessRuleException(
                    "Đã đạt giới hạn chuyển tiền hàng ngày", "DAILY_LIMIT_EXCEEDED");
        }
    }

    public void validateAccountActive(String status) {
        if (!"ACTIVE".equals(status)) {
            throw new BusinessRuleException("Tài khoản đã bị đóng băng", "ACCOUNT_FROZEN");
        }
    }
}
