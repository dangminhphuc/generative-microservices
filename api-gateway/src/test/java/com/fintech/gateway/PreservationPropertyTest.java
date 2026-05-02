package com.fintech.gateway;

import com.fintech.gateway.filter.JwtAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preservation Property Tests — Task 2
 *
 * PURPOSE: Capture existing CORRECT behavior that must NOT be broken by any fix.
 * These tests MUST PASS on the current unfixed code — they document the baseline.
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.6, 3.7
 *
 * Subtasks covered:
 *   2.1 — Valid JWT token flow (headers injected, chain forwarded)
 *   2.2 — Public path bypass (no token required for PUBLIC_PATHS)
 *   2.3 — Existing routes routing (YAML routes unchanged)
 *   2.4 — Non-internal path not blocked (no 404 from blocking rule)
 *   2.5 — CORS dev environment fallback (localhost:3000 default)
 *   2.6 — Actuator endpoints exposed (health, metrics, prometheus)
 */
class PreservationPropertyTest {

    // JWT secret from application.yml default value
    private static final String JWT_SECRET =
            "fintech-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator"
    );

    // Internal path pattern — used in 2.4 to verify non-internal paths are NOT matched
    private static final Pattern INTERNAL_PATTERN =
            Pattern.compile("^/([^/]+/)?internal(/.*)?$");

    private SecretKey secretKey;
    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        jwtFilter = new JwtAuthenticationFilter(JWT_SECRET);
    }

    // =========================================================================
    // 2.1 Preservation: Valid JWT token flow
    // Validates: Requirements 3.1
    // =========================================================================

    /**
     * Property 2: Preservation — Valid JWT Token Forwarding
     *
     * For all valid JWT tokens (correct signature, not expired), the gateway filter
     * MUST inject X-User-Id and X-User-Email headers and call chain.filter() (forward).
     *
     * Validates: Requirements 3.1
     */
    @Test
    void preservation_validJwtToken_filterInjectsHeadersAndForwards() {
        // Arrange: build a valid, non-expired JWT token
        String userId = "user-123";
        String userEmail = "user@example.com";
        String validToken = buildValidToken(userId, userEmail, 3600_000L);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/accounts/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Track whether chain.filter() was called (i.e., request was forwarded)
        boolean[] chainCalled = {false};
        String[] injectedUserId = {null};
        String[] injectedEmail = {null};

        // Act: run the filter with a chain that captures the mutated request
        Mono<Void> result = jwtFilter.filter(exchange, chain -> {
            chainCalled[0] = true;
            injectedUserId[0] = chain.getRequest().getHeaders().getFirst("X-User-Id");
            injectedEmail[0] = chain.getRequest().getHeaders().getFirst("X-User-Email");
            return Mono.empty();
        });

        result.block();

        // Assert: chain was called (request forwarded, not blocked)
        assertThat(chainCalled[0])
                .as("Preservation 2.1: valid JWT must cause chain.filter() to be called (request forwarded)")
                .isTrue();

        // Assert: X-User-Id header injected with correct value
        assertThat(injectedUserId[0])
                .as("Preservation 2.1: X-User-Id header must be injected from JWT subject")
                .isEqualTo(userId);

        // Assert: X-User-Email header injected with correct value
        assertThat(injectedEmail[0])
                .as("Preservation 2.1: X-User-Email header must be injected from JWT email claim")
                .isEqualTo(userEmail);

        // Assert: response is NOT 401 (filter did not block)
        assertThat(exchange.getResponse().getStatusCode())
                .as("Preservation 2.1: valid JWT must not result in 401")
                .isNotEqualTo(HttpStatus.UNAUTHORIZED);

        System.out.println("[PRESERVATION 2.1] Valid JWT → chain.filter() called, X-User-Id=" + injectedUserId[0]
                + ", X-User-Email=" + injectedEmail[0]);
    }

    /**
     * Property 2: Preservation — Valid JWT with multiple user IDs
     *
     * For all valid JWT tokens with different subjects, the correct subject is always
     * injected as X-User-Id. This is a parameterized property test.
     *
     * Validates: Requirements 3.1
     */
    @ParameterizedTest
    @ValueSource(strings = {"user-001", "user-abc", "admin-999", "service-account-1"})
    void preservation_validJwtToken_correctSubjectInjectedForAllUserIds(String userId) {
        String userEmail = userId + "@fintech.com";
        String validToken = buildValidToken(userId, userEmail, 3600_000L);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/accounts/balance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String[] capturedUserId = {null};

        Mono<Void> result = jwtFilter.filter(exchange, chain -> {
            capturedUserId[0] = chain.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.empty();
        });

        result.block();

        assertThat(capturedUserId[0])
                .as("Preservation 2.1: X-User-Id must equal JWT subject for userId=%s", userId)
                .isEqualTo(userId);

        System.out.println("[PRESERVATION 2.1] userId=" + userId + " → X-User-Id=" + capturedUserId[0] + " ✓");
    }

    // =========================================================================
    // 2.2 Preservation: Public path bypass
    // Validates: Requirements 3.2
    // =========================================================================

    /**
     * Property 2: Preservation — Public Path No-Auth
     *
     * For all paths in PUBLIC_PATHS, requests without any Authorization header
     * must NOT be blocked by the JWT filter (must not return 401).
     *
     * Validates: Requirements 3.2
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/refresh",
        "/actuator",
        "/actuator/health",
        "/actuator/metrics",
        "/actuator/prometheus"
    })
    void preservation_publicPath_requestWithoutTokenIsNotBlocked(String publicPath) {
        // Arrange: request with NO Authorization header
        MockServerHttpRequest request = MockServerHttpRequest
                .get(publicPath)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean[] chainCalled = {false};

        // Act
        Mono<Void> result = jwtFilter.filter(exchange, chain -> {
            chainCalled[0] = true;
            return Mono.empty();
        });

        result.block();

        // Assert: chain was called (filter bypassed for public path)
        assertThat(chainCalled[0])
                .as("Preservation 2.2: public path '%s' must bypass JWT filter (chain.filter() called)", publicPath)
                .isTrue();

        // Assert: response is NOT 401
        assertThat(exchange.getResponse().getStatusCode())
                .as("Preservation 2.2: public path '%s' must not return 401", publicPath)
                .isNotEqualTo(HttpStatus.UNAUTHORIZED);

        System.out.println("[PRESERVATION 2.2] Public path " + publicPath + " → NOT blocked (chain called) ✓");
    }

    /**
     * Property 2: Preservation — POST to public path also bypassed
     *
     * POST /api/auth/login without token must not be blocked (login is a public endpoint).
     *
     * Validates: Requirements 3.2
     */
    @Test
    void preservation_publicPath_postLoginWithoutTokenIsNotBlocked() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean[] chainCalled = {false};

        Mono<Void> result = jwtFilter.filter(exchange, chain -> {
            chainCalled[0] = true;
            return Mono.empty();
        });

        result.block();

        assertThat(chainCalled[0])
                .as("Preservation 2.2: POST /api/auth/login without token must bypass JWT filter")
                .isTrue();

        System.out.println("[PRESERVATION 2.2] POST /api/auth/login without token → NOT blocked ✓");
    }

    // =========================================================================
    // 2.3 Preservation: Existing routes routing
    // Validates: Requirements 3.3
    // =========================================================================

    /**
     * Property 2: Preservation — Route Forwarding Unchanged
     *
     * For all non-internal paths matching existing routes, the routing configuration
     * in application.yml must remain unchanged. Verified by static YAML analysis.
     *
     * Validates: Requirements 3.3
     */
    @Test
    void preservation_existingRoutes_accountServiceRoutePresent() throws Exception {
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(applicationYml);

        // Route: /api/accounts/** → lb://account-service
        assertThat(yaml)
                .as("Preservation 2.3: account-service route must be present in application.yml")
                .contains("id: account-service")
                .contains("uri: lb://account-service")
                .contains("Path=/api/accounts/**");

        System.out.println("[PRESERVATION 2.3] /api/accounts/** → lb://account-service route present ✓");
    }

    @Test
    void preservation_existingRoutes_transferServiceRoutePresent() throws Exception {
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(applicationYml);

        // Route: /api/transfers/** → lb://transfer-service
        assertThat(yaml)
                .as("Preservation 2.3: transfer-service route must be present in application.yml")
                .contains("id: transfer-service")
                .contains("uri: lb://transfer-service")
                .contains("Path=/api/transfers/**");

        System.out.println("[PRESERVATION 2.3] /api/transfers/** → lb://transfer-service route present ✓");
    }

    @Test
    void preservation_existingRoutes_transactionHistoryServiceRoutePresent() throws Exception {
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(applicationYml);

        // Route: /api/transactions/** → lb://transaction-history-service
        assertThat(yaml)
                .as("Preservation 2.3: transaction-history-service route must be present in application.yml")
                .contains("id: transaction-history-service")
                .contains("uri: lb://transaction-history-service")
                .contains("Path=/api/transactions/**");

        System.out.println("[PRESERVATION 2.3] /api/transactions/** → lb://transaction-history-service route present ✓");
    }

    @Test
    void preservation_existingRoutes_authServiceRoutePresent() throws Exception {
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(applicationYml);

        // Route: /api/auth/** → lb://account-service
        assertThat(yaml)
                .as("Preservation 2.3: account-service-auth route must be present in application.yml")
                .contains("id: account-service-auth")
                .contains("Path=/api/auth/**");

        System.out.println("[PRESERVATION 2.3] /api/auth/** → account-service-auth route present ✓");
    }

    // =========================================================================
    // 2.4 Preservation: Non-internal path not blocked
    // Validates: Requirements 3.3
    // =========================================================================

    /**
     * Property 2: Preservation — Non-Internal Path Passthrough
     *
     * For all paths NOT matching ^/([^/]+/)?internal(/.*)?$, the gateway must NOT
     * return 404 from the block-internal-endpoints route (because that route doesn't
     * exist yet on unfixed code).
     *
     * This is verified by:
     * 1. Confirming the blocking route does NOT exist in application.yml (unfixed code)
     * 2. Confirming the non-internal paths do NOT match the internal pattern
     *
     * Validates: Requirements 3.3
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "/api/accounts/123",
        "/api/accounts/balance",
        "/api/transfers/456",
        "/api/transactions/789",
        "/api/auth/login",
        "/actuator/health"
    })
    void preservation_nonInternalPath_doesNotMatchInternalPattern(String path) {
        boolean matchesInternal = INTERNAL_PATTERN.matcher(path).matches();

        assertThat(matchesInternal)
                .as("Preservation 2.4: path '%s' must NOT match internal pattern (should not be blocked)", path)
                .isFalse();

        System.out.println("[PRESERVATION 2.4] Path " + path + " → does NOT match internal pattern ✓");
    }

    /**
     * Property 2: Preservation — No blocking route exists on unfixed code
     *
     * On unfixed code, there is no block-internal-endpoints route, so non-internal
     * paths cannot be accidentally blocked by it.
     *
     * Validates: Requirements 3.3
     */
    @Test
    void preservation_nonInternalPath_noBlockingRouteExistsOnUnfixedCode() throws Exception {
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(applicationYml);

        // On unfixed code: no blocking route exists
        boolean hasBlockingRoute = yaml.contains("block-internal-endpoints")
                || (yaml.contains("/internal/**") && yaml.contains("SetStatus=404"));

        assertThat(hasBlockingRoute)
                .as("Preservation 2.4: on unfixed code, no block-internal-endpoints route should exist " +
                    "(so non-internal paths cannot be accidentally blocked by it)")
                .isFalse();

        System.out.println("[PRESERVATION 2.4] No block-internal-endpoints route on unfixed code → non-internal paths safe ✓");
    }

    /**
     * Property 2: Preservation — JWT filter does NOT block non-internal paths with valid token
     *
     * For non-internal paths with a valid JWT, the filter forwards (does not return 404).
     *
     * Validates: Requirements 3.3
     */
    @ParameterizedTest
    @ValueSource(strings = {"/api/accounts/123", "/api/transfers/456", "/api/transactions/789"})
    void preservation_nonInternalPath_jwtFilterForwardsWithValidToken(String path) {
        String validToken = buildValidToken("user-1", "user@test.com", 3600_000L);

        MockServerHttpRequest request = MockServerHttpRequest
                .get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean[] chainCalled = {false};

        Mono<Void> result = jwtFilter.filter(exchange, chain -> {
            chainCalled[0] = true;
            return Mono.empty();
        });

        result.block();

        assertThat(chainCalled[0])
                .as("Preservation 2.4: non-internal path '%s' with valid JWT must be forwarded by filter", path)
                .isTrue();

        System.out.println("[PRESERVATION 2.4] " + path + " with valid JWT → forwarded (not blocked) ✓");
    }

    // =========================================================================
    // 2.5 Preservation: CORS dev environment fallback
    // Validates: Requirements 3.7
    // =========================================================================

    /**
     * Property 2: Preservation — CORS Default Origin
     *
     * When GATEWAY_CORS_ALLOWED_ORIGINS is not set, the SecurityConfig must fall back
     * to http://localhost:3000 as the allowed origin. On unfixed code, this is the
     * hardcoded value — so the fallback is always active.
     *
     * Verified by static analysis of SecurityConfig.java source code.
     *
     * Validates: Requirements 3.7
     */
    @Test
    void preservation_corsDevFallback_localhostOriginPresentInSecurityConfig() throws Exception {
        Path securityConfigFile = Paths.get(
                "src/main/java/com/fintech/gateway/config/SecurityConfig.java");
        String sourceCode = Files.readString(securityConfigFile);

        // On unfixed code: http://localhost:3000 is hardcoded — dev fallback always works
        assertThat(sourceCode)
                .as("Preservation 2.5: SecurityConfig must contain http://localhost:3000 as allowed origin " +
                    "(dev environment fallback must work)")
                .contains("http://localhost:3000");

        System.out.println("[PRESERVATION 2.5] SecurityConfig contains http://localhost:3000 → dev CORS fallback active ✓");
    }

    /**
     * Property 2: Preservation — CORS config includes required methods and headers
     *
     * The CORS configuration must include the standard HTTP methods and headers
     * that the frontend relies on. These must not be removed by any fix.
     *
     * Validates: Requirements 3.7
     */
    @Test
    void preservation_corsDevFallback_requiredMethodsAndHeadersPresent() throws Exception {
        Path securityConfigFile = Paths.get(
                "src/main/java/com/fintech/gateway/config/SecurityConfig.java");
        String sourceCode = Files.readString(securityConfigFile);

        // Required HTTP methods
        assertThat(sourceCode)
                .as("Preservation 2.5: CORS config must include GET method")
                .contains("\"GET\"");
        assertThat(sourceCode)
                .as("Preservation 2.5: CORS config must include POST method")
                .contains("\"POST\"");
        assertThat(sourceCode)
                .as("Preservation 2.5: CORS config must include OPTIONS method")
                .contains("\"OPTIONS\"");

        // Required headers
        assertThat(sourceCode)
                .as("Preservation 2.5: CORS config must include Authorization header")
                .contains("\"Authorization\"");
        assertThat(sourceCode)
                .as("Preservation 2.5: CORS config must include Content-Type header")
                .contains("\"Content-Type\"");

        // Credentials support
        assertThat(sourceCode)
                .as("Preservation 2.5: CORS config must allow credentials")
                .contains("setAllowCredentials(true)");

        System.out.println("[PRESERVATION 2.5] CORS methods (GET, POST, OPTIONS), headers (Authorization, Content-Type), credentials=true ✓");
    }

    /**
     * Property 2: Preservation — CORS CorsWebFilter bean is registered
     *
     * The corsWebFilter() bean must remain registered in SecurityConfig.
     *
     * Validates: Requirements 3.7
     */
    @Test
    void preservation_corsDevFallback_corsWebFilterBeanPresent() throws Exception {
        Path securityConfigFile = Paths.get(
                "src/main/java/com/fintech/gateway/config/SecurityConfig.java");
        String sourceCode = Files.readString(securityConfigFile);

        assertThat(sourceCode)
                .as("Preservation 2.5: CorsWebFilter bean must be present in SecurityConfig")
                .contains("CorsWebFilter")
                .contains("@Bean");

        System.out.println("[PRESERVATION 2.5] CorsWebFilter @Bean present in SecurityConfig ✓");
    }

    // =========================================================================
    // 2.6 Preservation: Actuator endpoints exposed
    // Validates: Requirements 3.6
    // =========================================================================

    /**
     * Property 2: Preservation — Actuator Availability
     *
     * The management.endpoints.web.exposure.include config must expose
     * health, info, metrics, and prometheus endpoints.
     *
     * Verified by static analysis of application.yml.
     *
     * Validates: Requirements 3.6
     */
    @Test
    void preservation_actuatorEndpoints_healthExposedInConfig() throws Exception {
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(applicationYml);

        assertThat(yaml)
                .as("Preservation 2.6: management.endpoints.web.exposure.include must contain 'health'")
                .contains("health");

        System.out.println("[PRESERVATION 2.6] /actuator/health exposed in management config ✓");
    }

    @Test
    void preservation_actuatorEndpoints_metricsExposedInConfig() throws Exception {
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(applicationYml);

        assertThat(yaml)
                .as("Preservation 2.6: management.endpoints.web.exposure.include must contain 'metrics'")
                .contains("metrics");

        System.out.println("[PRESERVATION 2.6] /actuator/metrics exposed in management config ✓");
    }

    @Test
    void preservation_actuatorEndpoints_prometheusExposedInConfig() throws Exception {
        Path applicationYml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(applicationYml);

        assertThat(yaml)
                .as("Preservation 2.6: management.endpoints.web.exposure.include must contain 'prometheus'")
                .contains("prometheus");

        System.out.println("[PRESERVATION 2.6] /actuator/prometheus exposed in management config ✓");
    }

    /**
     * Property 2: Preservation — Actuator paths bypass JWT filter
     *
     * /actuator and /actuator/** are in PUBLIC_PATHS, so they bypass the JWT filter.
     * This must remain true after any fix.
     *
     * Validates: Requirements 3.6
     */
    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/health", "/actuator/metrics", "/actuator/prometheus"})
    void preservation_actuatorEndpoints_bypassJwtFilter(String actuatorPath) {
        // No Authorization header — actuator paths must bypass filter
        MockServerHttpRequest request = MockServerHttpRequest
                .get(actuatorPath)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        boolean[] chainCalled = {false};

        Mono<Void> result = jwtFilter.filter(exchange, chain -> {
            chainCalled[0] = true;
            return Mono.empty();
        });

        result.block();

        assertThat(chainCalled[0])
                .as("Preservation 2.6: actuator path '%s' must bypass JWT filter (no token required)", actuatorPath)
                .isTrue();

        assertThat(exchange.getResponse().getStatusCode())
                .as("Preservation 2.6: actuator path '%s' must not return 401", actuatorPath)
                .isNotEqualTo(HttpStatus.UNAUTHORIZED);

        System.out.println("[PRESERVATION 2.6] " + actuatorPath + " → bypasses JWT filter ✓");
    }

    /**
     * Property 2: Preservation — Actuator PUBLIC_PATHS entry present in filter source
     *
     * The JwtAuthenticationFilter source must contain /actuator in its PUBLIC_PATHS list.
     *
     * Validates: Requirements 3.6
     */
    @Test
    void preservation_actuatorEndpoints_actuatorInPublicPathsOfFilter() throws Exception {
        Path filterFile = Paths.get(
                "src/main/java/com/fintech/gateway/filter/JwtAuthenticationFilter.java");
        String sourceCode = Files.readString(filterFile);

        assertThat(sourceCode)
                .as("Preservation 2.6: JwtAuthenticationFilter must have /actuator in PUBLIC_PATHS")
                .contains("\"/actuator\"");

        System.out.println("[PRESERVATION 2.6] /actuator present in JwtAuthenticationFilter.PUBLIC_PATHS ✓");
    }

    // =========================================================================
    // Summary
    // =========================================================================

    /**
     * Summary: prints all preservation properties confirmed on unfixed code.
     */
    @Test
    void preservationSummary_allPropertiesHoldOnUnfixedCode() throws Exception {
        System.out.println("=================================================================");
        System.out.println("PRESERVATION PROPERTY SUMMARY — api-gateway (unfixed code)");
        System.out.println("=================================================================");

        // 2.1 Valid JWT flow
        String validToken = buildValidToken("user-1", "user@test.com", 3600_000L);
        MockServerHttpRequest req = MockServerHttpRequest.get("/api/accounts/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken).build();
        MockServerWebExchange ex = MockServerWebExchange.from(req);
        boolean[] chainCalled = {false};
        jwtFilter.filter(ex, chain -> { chainCalled[0] = true; return Mono.empty(); }).block();
        System.out.println("[2.1] Valid JWT → chain forwarded: " + chainCalled[0]);
        assertThat(chainCalled[0]).as("2.1 valid JWT forwarded").isTrue();

        // 2.2 Public path bypass
        MockServerHttpRequest loginReq = MockServerHttpRequest.post("/api/auth/login").build();
        MockServerWebExchange loginEx = MockServerWebExchange.from(loginReq);
        boolean[] loginChain = {false};
        jwtFilter.filter(loginEx, chain -> { loginChain[0] = true; return Mono.empty(); }).block();
        System.out.println("[2.2] POST /api/auth/login no token → chain forwarded: " + loginChain[0]);
        assertThat(loginChain[0]).as("2.2 public path bypassed").isTrue();

        // 2.3 Routes present
        Path yml = Paths.get("src/main/resources/application.yml");
        String yaml = Files.readString(yml);
        boolean routesPresent = yaml.contains("lb://account-service")
                && yaml.contains("lb://transfer-service")
                && yaml.contains("lb://transaction-history-service");
        System.out.println("[2.3] All service routes present: " + routesPresent);
        assertThat(routesPresent).as("2.3 all routes present").isTrue();

        // 2.4 No blocking route on unfixed code
        boolean noBlockingRoute = !yaml.contains("block-internal-endpoints");
        System.out.println("[2.4] No block-internal-endpoints route (unfixed): " + noBlockingRoute);
        assertThat(noBlockingRoute).as("2.4 no blocking route on unfixed code").isTrue();

        // 2.5 CORS localhost:3000 fallback
        Path secConfig = Paths.get("src/main/java/com/fintech/gateway/config/SecurityConfig.java");
        String secSrc = Files.readString(secConfig);
        boolean corsDefault = secSrc.contains("http://localhost:3000");
        System.out.println("[2.5] CORS localhost:3000 fallback present: " + corsDefault);
        assertThat(corsDefault).as("2.5 CORS dev fallback").isTrue();

        // 2.6 Actuator exposed
        boolean actuatorExposed = yaml.contains("health") && yaml.contains("metrics") && yaml.contains("prometheus");
        System.out.println("[2.6] Actuator endpoints exposed (health, metrics, prometheus): " + actuatorExposed);
        assertThat(actuatorExposed).as("2.6 actuator endpoints exposed").isTrue();

        System.out.println("=================================================================");
        System.out.println("ALL PRESERVATION PROPERTIES HOLD ON UNFIXED CODE ✓");
        System.out.println("=================================================================");
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Build a valid, signed JWT token using the same secret as the gateway filter.
     *
     * @param subject   the user ID (JWT subject)
     * @param email     the user email (JWT "email" claim)
     * @param ttlMillis time-to-live in milliseconds
     * @return signed JWT string
     */
    private String buildValidToken(String subject, String email, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiry = new Date(nowMillis + ttlMillis);

        return Jwts.builder()
                .subject(subject)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }
}
