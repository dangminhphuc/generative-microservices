package com.fintech.transfer.infrastructure.adapter.out.messaging;

import com.fintech.common.event.BaseDomainEvent;
import com.fintech.transfer.domain.port.out.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TRANSFER_EVENTS_TOPIC = "transfer-events";

    private final KafkaTemplate<String, BaseDomainEvent> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, BaseDomainEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(BaseDomainEvent event) {
        kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, event.getAggregateId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {}: {}", event.getEventType(), ex.getMessage());
                    } else {
                        log.info("Published event {} for aggregate {}", event.getEventType(), event.getAggregateId());
                    }
                });
    }
}
