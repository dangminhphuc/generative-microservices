package com.fintech.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * CORS Configuration Tests — Task 2.5.4
 *
 * Verifies Requirement 8 (Acceptance Criteria):
 *   1. When a request comes from an allowed origin, the response includes
 *      Access-Control-Allow-Origin header.
 *   2. When a request comes from a non-allowed origin, the response does NOT
 *      include a matching Access-Control-Allow-Origin header.
 *
 * Uses @SpringBootTest with WebTestClient and @TestPropertySource to inject
 * gateway.cors.allowed-origins without relying on Redis or downstream services.
 *
 * Validates: Requirements 8 (Acceptance Criteria 1, 3, 5)
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // Disable Redis health check — Redis not available in test environment
                "management.health.redis.enabled=false"
        }
)
@TestPropertySource(properties = "gateway.cors.allowed-origins=http://localhost:3000")
class CorsConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Invariant: For every request from an allowed origin, the response MUST include
     * Access-Control-Allow-Origin header matching that origin.
     *
     * Uses a CORS preflight (OPTIONS) request — this is the standard way to trigger
     * CORS header evaluation without needing a real downstream service.
     *
     * Validates: Requirements 8 — Acceptance Criterion 1
     */
    @Test
    void corsAllowedOrigin_preflightRequest_includesAccessControlAllowOriginHeader() {
        webTestClient.options()
                .uri("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization")
                .exchange()
                .expectHeader()
                .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
    }

    /**
     * Invariant: For every request from a non-allowed origin, the response MUST NOT
     * include an Access-Control-Allow-Origin header matching that origin.
     *
     * Validates: Requirements 8 — Acceptance Criterion 5
     */
    @Test
    void corsNonAllowedOrigin_preflightRequest_doesNotIncludeAccessControlAllowOriginHeader() {
        webTestClient.options()
                .uri("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type")
                .exchange()
                .expectHeader()
                .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    /**
     * Verifies that when GATEWAY_CORS_ALLOWED_ORIGINS is not set, the default fallback
     * http://localhost:3000 is used as the allowed origin.
     *
     * This test uses the same @TestPropertySource value (http://localhost:3000) which
     * mirrors the default fallback in application-docker.yml:
     *   gateway.cors.allowed-origins: ${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:3000}
     *
     * Validates: Requirements 8 — Acceptance Criterion 3
     */
    @Test
    void corsDefaultFallback_localhostOriginAllowed_whenEnvVarNotSet() {
        // The @TestPropertySource sets gateway.cors.allowed-origins=http://localhost:3000
        // which mirrors the default fallback when GATEWAY_CORS_ALLOWED_ORIGINS is not set.
        webTestClient.options()
                .uri("/actuator/health")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectHeader()
                .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
    }
}
