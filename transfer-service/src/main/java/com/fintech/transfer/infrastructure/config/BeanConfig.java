package com.fintech.transfer.infrastructure.config;

import com.fintech.transfer.domain.service.TransferValidationService;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableJpaAuditing
public class BeanConfig {

    @Bean
    public TransferValidationService transferValidationService(
            @Value("${transfer.minimum-amount}") long minimumAmount,
            @Value("${transfer.daily-limit}") long dailyLimit) {
        return new TransferValidationService(minimumAmount, dailyLimit);
    }

    @Bean
    public NewTopic transferEventsTopic() {
        return TopicBuilder.name("transfer-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
