package com.fintech.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Bug Condition Exploration Tests — Task 1
 *
 * PURPOSE: Confirm each bug condition EXISTS on unfixed code.
 * These tests are expected to FAIL (or surface counterexamples) on the current codebase.
 * DO NOT fix the code or the tests when they fail — failure IS the expected outcome.
 *
 * Each test method corresponds to one bug condition from the spec:
 *   - Issue 1: No integration test class
 *   - Issue 2: Internal endpoints exposed via discovery locator
 *   - Issue 3: JWT exceptions caught silently (no logging)
 *   - Issue 4: CORS origin hardcoded in source code
 */
class BugConditionExplorationTest {

    // -------------------------------------------------------------------------
    // Issue 1 — isBugCondition_NoTest
    // -------------------------------------------------------------------------

    /**
     * Bug Condition: testClassExists("ApiGatewayApplicationTests") = FALSE
     *
     * Counterexample: "Không có integration test nào xác nhận context load,
     * health endpoint, JWT filter"
     *
     * EXPECTED OUTCOME: This assertion PASSES (confirms the bug condition holds).
     * When the fix is applied (task 3), ApiGatewayApplicationTests.java will exist
     * and this assertion will FAIL — indicating the bug is resolved.
     */
    @Test
    void isBugCondition_NoTest_integrationTestClassDoesNotExist() {
        // Resolve the test source root relative to the project module
        Path testSourceRoot = Paths.get("src/test/java/com/fintech/gateway");
        Path integrationTestFile = testSourceRoot.resolve("ApiGatewayApplicationTests.java");

        boolean testClassExists = Files.exists(integrationTestFile);

        // Bug condition: file must NOT exist
        assertThat(testClassExists)
                .as("BUG CONDITION (Issue 1): ApiGatewayApplicationTests.java should NOT exist on unfixed code. " +
                    "Counterexample: no test confirms context load, /actuator/health, JWT filter, or public path bypass.")
                .isFalse();

        // Log counterexample
        System.out.println("[COUNTEREXAMPLE Issue 1] testClassExists(\"ApiGatewayApplicationTests\") = FALSE");
        System.out.println("  → No integration test confirms: context load, health endpoint, JWT filter, public path");
    }

    // -------------------------------------------------------------------------
    // Issue 2 — isBugCondition_InternalExposed
    // -------------------------------------------------------------------------

    /**
     * Bug Condition: path MATCHES "^/([^/]+/)?internal(/.*)?$" AND gateway does NOT block it.
     *
     * Static verification: application.yml has NO route with predicate matching /internal/**
     * before the discovery locator routes. The discovery locator is enabled, which means
     * /account-service/internal/** is automatically forwarded to account-service.
     *
     * Counterexample: "GET /account-service/internal/accounts không bị block bởi gateway"
     *
     * EXPECTED OUTCOME: This assertion PASSES (confirms the bug condition holds).
     * When the fix is applied (task 4), a blocking route will exist and this assertion will FAIL.
     */
    @Test
    void isBugCondition_InternalExposed_noBlockingRouteInApplicationYml() throws Exception {
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yamlContent = Files.readString(applicationYml);

        // The internal-blocking route must NOT exist in unfixed code
        boolean hasBlockingRoute = yamlContent.contains("block-internal-endpoints")
                || (yamlContent.contains("/internal/**") && yamlContent.contains("SetStatus=404"));

        assertThat(hasBlockingRoute)
                .as("BUG CONDITION (Issue 2): No blocking route for /*/internal/** should exist in unfixed application.yml. " +
                    "Counterexample: GET /account-service/internal/accounts is NOT blocked by gateway — " +
                    "discovery locator forwards it to InternalAccountController.")
                .isFalse();

        // Confirm discovery locator is enabled (the mechanism that exposes internal paths)
        boolean discoveryLocatorEnabled = yamlContent.contains("locator:")
                && yamlContent.contains("enabled: true");

        assertThat(discoveryLocatorEnabled)
                .as("Discovery locator must be enabled for the bug to be exploitable")
                .isTrue();

        // Log counterexample
        System.out.println("[COUNTEREXAMPLE Issue 2] isBugCondition_InternalExposed: path matches ^/([^/]+/)?internal(/.*)?$ is NOT blocked");
        System.out.println("  → GET /account-service/internal/accounts → discovery locator forwards to downstream (no 404 from gateway)");
        System.out.println("  → GET /transfer-service/internal/settle  → discovery locator forwards to downstream (no 404 from gateway)");
        System.out.println("  → GET /internal/admin                    → discovery locator forwards to downstream (no 404 from gateway)");
    }

    /**
     * Additional static check: verify the internal path regex matches the expected paths.
     * This documents the exact paths that are vulnerable.
     */
    @Test
    void isBugCondition_InternalExposed_pathRegexMatchesVulnerablePaths() {
        Pattern internalPattern = Pattern.compile("^/([^/]+/)?internal(/.*)?$");

        String[] vulnerablePaths = {
            "/account-service/internal/accounts",
            "/account-service/internal/accounts/123",
            "/transfer-service/internal/settle",
            "/internal/admin",
            "/internal/",
        };

        String[] safePaths = {
            "/api/accounts/123",
            "/api/transfers/456",
            "/api/auth/login",
            "/actuator/health",
        };

        for (String path : vulnerablePaths) {
            assertThat(internalPattern.matcher(path).matches())
                    .as("Path '%s' should match the internal pattern (vulnerable)", path)
                    .isTrue();
        }

        for (String path : safePaths) {
            assertThat(internalPattern.matcher(path).matches())
                    .as("Path '%s' should NOT match the internal pattern (safe)", path)
                    .isFalse();
        }

        System.out.println("[COUNTEREXAMPLE Issue 2] Vulnerable paths confirmed by regex ^/([^/]+/)?internal(/.*)?$:");
        for (String path : vulnerablePaths) {
            System.out.println("  → " + path + " (EXPOSED — no gateway block)");
        }
    }

    // -------------------------------------------------------------------------
    // Issue 3 — isBugCondition_JwtException
    // -------------------------------------------------------------------------

    /**
     * Bug Condition: catch (Exception e) without log statement in JwtAuthenticationFilter.
     *
     * Static verification: JwtAuthenticationFilter.java has a single broad catch(Exception e)
     * with no Logger field and no log.warn/log.error call inside the catch block.
     *
     * Counterexample: "Token hết hạn → 401 nhưng không có WARN log;
     *                  NullPointerException → 401 nhưng không có ERROR log với stack trace"
     *
     * EXPECTED OUTCOME: This assertion PASSES (confirms the bug condition holds).
     * When the fix is applied (task 5), the catch block will be split and logging added.
     */
    @Test
    void isBugCondition_JwtException_catchBlockIsUndifferentiatedAndSilent() throws Exception {
        Path filterFile = Paths.get(
            "src/main/java/com/fintech/gateway/filter/JwtAuthenticationFilter.java");
        String sourceCode = Files.readString(filterFile);

        // Bug condition 1: no Logger field declared
        boolean hasLoggerField = sourceCode.contains("Logger log")
                || sourceCode.contains("Logger LOG")
                || sourceCode.contains("LoggerFactory.getLogger");

        assertThat(hasLoggerField)
                .as("BUG CONDITION (Issue 3): JwtAuthenticationFilter must NOT have a Logger on unfixed code. " +
                    "Counterexample: expired token → 401 but no WARN log emitted.")
                .isFalse();

        // Bug condition 2: single broad catch(Exception e) — not split into JwtException + Exception
        boolean hasSplitCatch = sourceCode.contains("catch (JwtException");

        assertThat(hasSplitCatch)
                .as("BUG CONDITION (Issue 3): catch block must NOT be split into JwtException vs Exception on unfixed code. " +
                    "Counterexample: NullPointerException during parse → 401 but no ERROR log with stack trace.")
                .isFalse();

        // Bug condition 3: no log call inside catch block
        boolean hasLogInCatch = sourceCode.contains("log.warn") || sourceCode.contains("log.error");

        assertThat(hasLogInCatch)
                .as("BUG CONDITION (Issue 3): No log.warn or log.error call should exist in unfixed code.")
                .isFalse();

        // Log counterexample
        System.out.println("[COUNTEREXAMPLE Issue 3] isBugCondition_JwtException: catch (Exception e) — no Logger, no log call");
        System.out.println("  → ExpiredJwtException  → 401 returned, but NO WARN log (security event invisible)");
        System.out.println("  → MalformedJwtException → 401 returned, but NO WARN log (attack not auditable)");
        System.out.println("  → NullPointerException  → 401 returned, but NO ERROR log with stack trace (bug undebuggable)");
    }

    // -------------------------------------------------------------------------
    // Issue 4 — isBugCondition_CorsHardcode
    // -------------------------------------------------------------------------

    /**
     * Bug Condition: allowedOriginsSource = "HARDCODED_IN_SOURCE_CODE"
     *
     * Static verification: SecurityConfig.java uses List.of("http://localhost:3000") directly,
     * with no @Value injection and no property in application.yml for CORS origins.
     *
     * Counterexample: "CORS preflight từ https://app.fintech.com bị block dù đã set env var
     *                  GATEWAY_CORS_ALLOWED_ORIGINS=https://app.fintech.com"
     *
     * EXPECTED OUTCOME: This assertion PASSES (confirms the bug condition holds).
     * When the fix is applied (task 6), @Value injection will be used.
     */
    @Test
    void isBugCondition_CorsHardcode_allowedOriginsIsLiteralInSourceCode() throws Exception {
        Path securityConfigFile = Paths.get(
            "src/main/java/com/fintech/gateway/config/SecurityConfig.java");
        String sourceCode = Files.readString(securityConfigFile);

        // Bug condition 1: hardcoded origin literal present
        boolean hasHardcodedOrigin = sourceCode.contains("\"http://localhost:3000\"");

        assertThat(hasHardcodedOrigin)
                .as("BUG CONDITION (Issue 4): SecurityConfig must contain hardcoded 'http://localhost:3000' on unfixed code. " +
                    "Counterexample: CORS preflight from https://app.fintech.com is BLOCKED even when " +
                    "GATEWAY_CORS_ALLOWED_ORIGINS=https://app.fintech.com is set.")
                .isTrue();

        // Bug condition 2: no @Value injection for CORS origins
        boolean hasValueAnnotation = sourceCode.contains("@Value") &&
                (sourceCode.contains("cors.allowed-origins") || sourceCode.contains("GATEWAY_CORS"));

        assertThat(hasValueAnnotation)
                .as("BUG CONDITION (Issue 4): SecurityConfig must NOT have @Value injection for CORS origins on unfixed code.")
                .isFalse();

        // Bug condition 3: application.yml has no gateway.cors property
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yamlContent = Files.readString(applicationYml);

        boolean hasCorsProperty = yamlContent.contains("gateway:") && yamlContent.contains("cors:");

        assertThat(hasCorsProperty)
                .as("BUG CONDITION (Issue 4): application.yml must NOT have gateway.cors property on unfixed code.")
                .isFalse();

        // Log counterexample
        System.out.println("[COUNTEREXAMPLE Issue 4] isBugCondition_CorsHardcode: allowedOriginsSource = HARDCODED_IN_SOURCE_CODE");
        System.out.println("  → Deploy with GATEWAY_CORS_ALLOWED_ORIGINS=https://app.fintech.com");
        System.out.println("  → OPTIONS https://app.fintech.com → BLOCKED (Access-Control-Allow-Origin: http://localhost:3000 only)");
        System.out.println("  → Docker/prod frontend at http://frontend:3000 → BLOCKED");
    }

    /**
     * Summary test: prints all confirmed counterexamples in one place for easy review.
     */
    @Test
    void bugConditionSummary_allFourIssuesConfirmed() throws Exception {
        System.out.println("=================================================================");
        System.out.println("BUG CONDITION EXPLORATION SUMMARY — api-gateway (unfixed code)");
        System.out.println("=================================================================");

        // Issue 1
        Path integrationTestFile = Paths.get("src/test/java/com/fintech/gateway/ApiGatewayApplicationTests.java");
        boolean issue1 = !Files.exists(integrationTestFile);
        System.out.printf("[Issue 1] isBugCondition_NoTest = %b%n", issue1);
        System.out.println("  Counterexample: ApiGatewayApplicationTests.java does not exist");

        // Issue 2
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(applicationYml);
        boolean issue2 = !yaml.contains("block-internal-endpoints") && yaml.contains("enabled: true");
        System.out.printf("[Issue 2] isBugCondition_InternalExposed = %b%n", issue2);
        System.out.println("  Counterexample: GET /account-service/internal/accounts not blocked by gateway");

        // Issue 3
        Path filterFile = Paths.get("src/main/java/com/fintech/gateway/filter/JwtAuthenticationFilter.java");
        String filterSrc = Files.readString(filterFile);
        boolean issue3 = !filterSrc.contains("LoggerFactory.getLogger") && !filterSrc.contains("catch (JwtException");
        System.out.printf("[Issue 3] isBugCondition_JwtException = %b%n", issue3);
        System.out.println("  Counterexample: ExpiredJwtException → 401 but no WARN log; NPE → 401 but no ERROR log");

        // Issue 4
        Path securityConfig = Paths.get("src/main/java/com/fintech/gateway/config/SecurityConfig.java");
        String secSrc = Files.readString(securityConfig);
        boolean issue4 = secSrc.contains("\"http://localhost:3000\"") && !secSrc.contains("@Value");
        System.out.printf("[Issue 4] isBugCondition_CorsHardcode = %b%n", issue4);
        System.out.println("  Counterexample: CORS preflight from https://app.fintech.com blocked despite env var");

        System.out.println("=================================================================");

        // All four must hold on unfixed code
        assertThat(issue1).as("Issue 1 bug condition").isTrue();
        assertThat(issue2).as("Issue 2 bug condition").isTrue();
        assertThat(issue3).as("Issue 3 bug condition").isTrue();
        assertThat(issue4).as("Issue 4 bug condition").isTrue();
    }
}
