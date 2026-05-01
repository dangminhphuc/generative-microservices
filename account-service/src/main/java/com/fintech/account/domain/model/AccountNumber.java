package com.fintech.account.domain.model;

import com.fintech.common.domain.BaseValueObject;
import java.util.Objects;
import java.util.regex.Pattern;

public final class AccountNumber extends BaseValueObject {

    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^\\d{10}$");

    private final String value;

    private AccountNumber(String value) {
        this.value = value;
    }

    public static AccountNumber of(String value) {
        Objects.requireNonNull(value, "Account number must not be null");
        if (!ACCOUNT_NUMBER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Account number must be exactly 10 digits");
        }
        return new AccountNumber(value);
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountNumber that = (AccountNumber) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
