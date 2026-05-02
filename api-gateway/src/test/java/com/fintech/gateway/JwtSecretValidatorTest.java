package com.fintech.gateway;

import com.fintech.gateway.config.JwtSecretValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JwtSecretValidator}.
 *
 * Covers three correctness properties from Requirement 1:
 *
 * <ol>
 *   <li>Profile {@code prod} + default secret → context MUST NOT load (IllegalStateException).</li>
 *   <li>Profile {@code prod} + custom secret  → context MUST load successfully.</li>
 *   <li>Profile {@code dev}  + default secret → context MUST load successfully (validator inactive).</li>
 * </ol>
 */
class JwtSecretValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtSecretValidator.class);

    // -----------------------------------------------------------------------
    // Task 1.1.5 — prod profile + default secret → context must NOT start
    // -----------------------------------------------------------------------

    /**
     * Verifies Acceptance Criterion 2:
     * WHEN api-gateway starts with profile "prod" AND jwt.secret equals the default value,
     * THEN it SHALL throw IllegalStateException and refuse to start.
     */
    @Test
    void prodProfile_defaultSecret_contextFailsToStart() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=" + JwtSecretValidator.DEFAULT_SECRET,
                        "spring.profiles.active=prod"
                )
                .withSystemProperties("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("JWT_SECRET");
                });
    }

    // -----------------------------------------------------------------------
    // Task 1.1.6 — prod profile + custom secret → context must start
    // -----------------------------------------------------------------------

    /**
     * Verifies Acceptance Criterion 3:
     * WHEN api-gateway starts with profile "prod" AND jwt.secret is a non-default value,
     * THEN it SHALL start successfully.
     */
    @Test
    void prodProfile_customSecret_contextStartsSuccessfully() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=my-super-secure-custom-secret-value-for-production-use-only",
                        "spring.profiles.active=prod"
                )
                .withSystemProperties("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasNotFailed());
    }

    // -----------------------------------------------------------------------
    // Task 1.1.7 — dev profile + default secret → context must start
    // -----------------------------------------------------------------------

    /**
     * Verifies Acceptance Criterion 4:
     * WHEN api-gateway starts with profile "dev" AND jwt.secret is the default value,
     * THEN it SHALL start successfully (JwtSecretValidator is NOT active on dev).
     */
    @Test
    void devProfile_defaultSecret_contextStartsSuccessfully() {
        // JwtSecretValidator is @Profile("prod") — it must NOT be registered on "dev".
        // We run the context WITHOUT activating the prod profile, so the validator bean
        // is simply not present and the context loads cleanly.
        new ApplicationContextRunner()
                .withUserConfiguration(JwtSecretValidator.class)
                .withPropertyValues(
                        "jwt.secret=" + JwtSecretValidator.DEFAULT_SECRET,
                        "spring.profiles.active=dev"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }
}
