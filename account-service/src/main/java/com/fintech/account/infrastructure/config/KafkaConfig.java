package com.fintech.account.infrastructure.config;

import com.fintech.common.event.BaseDomainEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    private static final String TRANSFER_EVENTS_DLT = "transfer-events.DLT";

    @Bean
    public NewTopic accountEventsTopic() {
        return TopicBuilder.name("account-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // 2.4.2 — DLT topic: 3 partitions, 1 replica
    @Bean
    public NewTopic transferEventsDltTopic() {
        return TopicBuilder.name(TRANSFER_EVENTS_DLT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // 2.4.3 — Route failed messages to DLT with same partition, log warning
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, BaseDomainEvent> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> {
                    log.warn("Publishing to DLT: topic={}, partition={}, key={}, error={}",
                            record.topic(), record.partition(), record.key(), ex.getMessage());
                    return new TopicPartition(TRANSFER_EVENTS_DLT, record.partition());
                });
    }

    // 2.4.4 — ExponentialBackOffWithMaxRetries(3): 1s → 2s → 4s, then DLT
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    // 2.4.5 — Override container factory to inject DefaultErrorHandler
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BaseDomainEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, BaseDomainEvent> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, BaseDomainEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}
