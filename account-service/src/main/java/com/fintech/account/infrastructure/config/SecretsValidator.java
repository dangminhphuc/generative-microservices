package com.fintech.account.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class SecretsValidator {

    private static final String DEFAULT_JWT_SECRET =
        "fintech-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";
    private static final String DEFAULT_INTERNAL_SECRET =
        "dev-internal-secret-for-local-only";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    @PostConstruct
    public void validate() {
        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                "[account-service] JWT_SECRET must not be the default value in production. " +
                "Set the JWT_SECRET environment variable.");
        }
        if (DEFAULT_INTERNAL_SECRET.equals(internalApiSecret)) {
            throw new IllegalStateException(
                "[account-service] INTERNAL_API_SECRET must not be the default value in production. " +
                "Set the INTERNAL_API_SECRET environment variable.");
        }
    }
}
