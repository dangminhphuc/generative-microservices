package com.fintech.transfer.domain.port.out;

import com.fintech.common.event.BaseDomainEvent;

public interface EventPublisher {
    void publish(BaseDomainEvent event);
}
