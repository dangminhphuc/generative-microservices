package com.fintech.account.domain.model;

import com.fintech.common.domain.BaseValueObject;
import java.util.Objects;
import java.util.regex.Pattern;

public final class PhoneNumber extends BaseValueObject {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    private final String value;

    private PhoneNumber(String value) {
        this.value = value;
    }

    public static PhoneNumber of(String value) {
        Objects.requireNonNull(value, "Phone number must not be null");
        String cleaned = value.replaceAll("[\\s-]", "");
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Invalid VN phone number format");
        }
        return new PhoneNumber(cleaned);
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhoneNumber that = (PhoneNumber) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value; }
}
