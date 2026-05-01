package com.fintech.common.event;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseDomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    protected BaseDomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.aggregateId = aggregateId;
    }

    protected BaseDomainEvent(UUID eventId, Instant occurredAt, String aggregateId) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.aggregateId = aggregateId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public abstract String getEventType();
}
