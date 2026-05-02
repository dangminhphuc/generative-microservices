package com.fintech.gateway;

import com.fintech.gateway.controller.FallbackController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FallbackController}.
 *
 * Validates: Requirements 6.2 — when circuit breaker is OPEN, API Gateway returns
 * HTTP 503 Service Unavailable with a JSON error body instead of forwarding to downstream.
 *
 * Tests cover:
 * <ul>
 *   <li>Response status is 503 SERVICE_UNAVAILABLE</li>
 *   <li>Response body contains "error" key with correct message</li>
 *   <li>Response body contains "code" key with SERVICE_UNAVAILABLE value</li>
 *   <li>Response body is not null and has exactly 2 entries</li>
 * </ul>
 */
class FallbackControllerTest {

    private FallbackController fallbackController;

    @BeforeEach
    void setUp() {
        fallbackController = new FallbackController();
    }

    /**
     * Verifies that the fallback endpoint returns HTTP 503 Service Unavailable.
     *
     * Validates: Requirements 6.2 — circuit breaker OPEN state returns 503
     */
    @Test
    void serviceUnavailable_returnsHttp503() {
        ResponseEntity<Map<String, String>> response = fallbackController.serviceUnavailable();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Verifies that the response body contains the "error" field with the correct message.
     *
     * Validates: Requirements 6.2 — JSON error body with human-readable message
     */
    @Test
    void serviceUnavailable_bodyContainsErrorMessage() {
        ResponseEntity<Map<String, String>> response = fallbackController.serviceUnavailable();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("error", "Service temporarily unavailable");
    }

    /**
     * Verifies that the response body contains the "code" field with SERVICE_UNAVAILABLE.
     *
     * Validates: Requirements 6.2 — JSON error body with machine-readable error code
     */
    @Test
    void serviceUnavailable_bodyContainsErrorCode() {
        ResponseEntity<Map<String, String>> response = fallbackController.serviceUnavailable();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("code", "SERVICE_UNAVAILABLE");
    }

    /**
     * Verifies that the response body has exactly 2 entries (no extra fields).
     *
     * Validates: Requirements 6.2 — response body matches the specified JSON structure exactly
     */
    @Test
    void serviceUnavailable_bodyHasExactlyTwoEntries() {
        ResponseEntity<Map<String, String>> response = fallbackController.serviceUnavailable();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }

    /**
     * Verifies that the response body is not null.
     *
     * Validates: Requirements 6.2 — clients must receive a JSON body, not an empty response
     */
    @Test
    void serviceUnavailable_bodyIsNotNull() {
        ResponseEntity<Map<String, String>> response = fallbackController.serviceUnavailable();

        assertThat(response.getBody()).isNotNull();
    }

    /**
     * Verifies that calling serviceUnavailable() multiple times always returns 503.
     * The fallback must be idempotent — every invocation returns the same status.
     *
     * Validates: Requirements 6.2 — consistent behavior when circuit is OPEN
     */
    @Test
    void serviceUnavailable_isIdempotent() {
        ResponseEntity<Map<String, String>> response1 = fallbackController.serviceUnavailable();
        ResponseEntity<Map<String, String>> response2 = fallbackController.serviceUnavailable();
        ResponseEntity<Map<String, String>> response3 = fallbackController.serviceUnavailable();

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        assertThat(response1.getBody()).isEqualTo(response2.getBody());
        assertThat(response2.getBody()).isEqualTo(response3.getBody());
    }
}
