package com.fintech.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Validates that the JWT secret is not the default insecure value when running in production.
 *
 * <p>This component is only active under the {@code prod} Spring profile. On startup it checks
 * whether {@code jwt.secret} still holds the well-known default value that is committed to source
 * control. If it does, the application refuses to start with a clear error message so that the
 * misconfiguration is caught immediately rather than silently allowing attackers to forge tokens.
 *
 * <p>Correctness invariant: for every startup on profile {@code prod}, if {@code jwt.secret}
 * equals the default value the application context MUST NOT load successfully.
 */
@Component
@Profile("prod")
public class JwtSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretValidator.class);

    /**
     * The well-known default secret committed to source control.
     * Any production deployment that still uses this value is insecure.
     */
    public static final String DEFAULT_SECRET =
            "fintech-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void validate() {
        if (DEFAULT_SECRET.equals(jwtSecret)) {
            String message =
                    "SECURITY VIOLATION: jwt.secret is set to the default insecure value. "
                    + "Set the JWT_SECRET environment variable to a cryptographically secure "
                    + "random value before starting the application in production.";
            log.error(message);
            throw new IllegalStateException(message);
        }
        log.info("JWT secret validation passed — non-default secret detected.");
    }
}
