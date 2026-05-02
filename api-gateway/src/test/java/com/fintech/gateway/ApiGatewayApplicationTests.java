package com.fintech.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for API Gateway — Task 3.1
 *
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4
 *
 * Fix for Issue 1 — Thiếu Integration Test
 * Bug_Condition: isBugCondition_NoTest — testClassExists("ApiGatewayApplicationTests") = FALSE
 * Expected_Behavior: Test class tồn tại với 4 test methods xác nhận context load, health, JWT filter, public path
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Verifies Spring context starts without exception.
     * If context fails to load (e.g., missing jwt.secret, misconfigured bean),
     * this test will fail before the method body even executes.
     *
     * Validates: Requirements 2.1
     */
    @Test
    void contextLoads() {
        // Context load failure throws exception before this line — empty body is sufficient
    }

    /**
     * Verifies /actuator/health returns HTTP 200 with "status":"UP".
     *
     * Validates: Requirements 2.2
     */
    @Test
    void healthEndpointReturnsUp() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("\"status\":\"UP\""));
    }

    /**
     * Verifies JwtAuthenticationFilter rejects requests to protected paths without a token.
     * GET /api/accounts/123 without Authorization header → HTTP 401.
     *
     * Validates: Requirements 2.3
     */
    @Test
    void jwtFilterRejectsUnauthorized() {
        webTestClient.get().uri("/api/accounts/123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Verifies public path /api/auth/login is not blocked by the JWT filter.
     * POST /api/auth/login without a token → any status except 401.
     * (Response may be 404/502 from downstream when service is not running in test — that is acceptable.)
     *
     * Validates: Requirements 2.4
     */
    @Test
    void publicPathAllowsNoToken() {
        webTestClient.post().uri("/api/auth/login")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
    }
}
