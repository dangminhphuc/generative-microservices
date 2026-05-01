package com.fintech.transfer.infrastructure.adapter.out.rest;

import com.fintech.common.dto.AccountInfoResponse;
import com.fintech.common.exception.ResourceNotFoundException;
import com.fintech.transfer.domain.port.out.AccountServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class AccountServiceRestClient implements AccountServicePort {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceRestClient.class);

    private final RestClient restClient;

    public AccountServiceRestClient(
            @Value("${account-service.url}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public AccountInfoResponse getAccountByNumber(String accountNumber) {
        try {
            return restClient.get()
                    .uri("/internal/accounts/{accountNumber}", accountNumber)
                    .retrieve()
                    .body(AccountInfoResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Account", accountNumber);
        } catch (Exception e) {
            log.error("Failed to call Account Service for account {}: {}", accountNumber, e.getMessage());
            throw new RuntimeException("Account Service unavailable", e);
        }
    }
}
