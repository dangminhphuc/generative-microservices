package com.fintech.account.domain.port.out;

import com.fintech.common.event.BaseDomainEvent;

public interface EventPublisher {
    void publish(BaseDomainEvent event);
}
