package com.fintech.common.domain;

/**
 * Base class for Value Objects.
 * Value Objects are immutable and compared by their attribute values.
 * Subclasses should be implemented as Java records or override equals/hashCode.
 */
public abstract class BaseValueObject {

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
