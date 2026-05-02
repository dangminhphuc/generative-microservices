package com.fintech.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Fallback controller for circuit breaker responses.
 *
 * <p>When a downstream service is unavailable or the circuit breaker is OPEN,
 * Spring Cloud Gateway forwards the request to {@code /fallback/service-unavailable}.
 * This controller returns a standardized 503 JSON response so clients receive a
 * meaningful error instead of a raw timeout or connection-refused error.
 *
 * <p>Validates: Requirements 6.2 — circuit breaker returns HTTP 503 with JSON error body.
 */
@RestController
public class FallbackController {

    /**
     * Returns HTTP 503 Service Unavailable with a JSON error body.
     *
     * <p>This endpoint is invoked by the {@code CircuitBreaker} gateway filter via
     * {@code fallbackUri: forward:/fallback/service-unavailable} when the circuit is OPEN
     * or the downstream service fails to respond.
     *
     * @return 503 response with {@code {"error": "Service temporarily unavailable", "code": "SERVICE_UNAVAILABLE"}}
     */
    @RequestMapping("/fallback/service-unavailable")
    public ResponseEntity<Map<String, String>> serviceUnavailable() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service temporarily unavailable",
                        "code", "SERVICE_UNAVAILABLE"
                ));
    }
}
