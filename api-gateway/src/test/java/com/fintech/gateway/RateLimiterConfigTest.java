package com.fintech.gateway;

import com.fintech.gateway.config.RateLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RateLimiterConfig}.
 *
 * Validates: Requirements 5.1, 5.2
 *
 * Tests cover:
 * <ul>
 *   <li>ipKeyResolver — resolves client remote IP address</li>
 *   <li>userKeyResolver — resolves X-User-Id header, falls back to "anonymous"</li>
 * </ul>
 */
class RateLimiterConfigTest {

    private RateLimiterConfig rateLimiterConfig;
    private KeyResolver ipKeyResolver;
    private KeyResolver userKeyResolver;

    @BeforeEach
    void setUp() {
        rateLimiterConfig = new RateLimiterConfig();
        ipKeyResolver = rateLimiterConfig.ipKeyResolver();
        userKeyResolver = rateLimiterConfig.userKeyResolver();
    }

    // -------------------------------------------------------------------------
    // ipKeyResolver tests
    // -------------------------------------------------------------------------

    /**
     * ipKeyResolver must return the client's remote IP address.
     *
     * Validates: Requirements 5.1 — rate limiting per IP for auth routes
     */
    @Test
    void ipKeyResolver_returnsRemoteIpAddress() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .remoteAddress(new InetSocketAddress("192.168.1.100", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = ipKeyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("192.168.1.100");
    }

    /**
     * ipKeyResolver must return loopback address for localhost requests.
     *
     * Validates: Requirements 5.1
     */
    @Test
    void ipKeyResolver_returnsLoopbackForLocalhost() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/register")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 54321))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = ipKeyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("127.0.0.1");
    }

    /**
     * Different IPs must produce different keys — rate limits are per-IP, not shared.
     *
     * Validates: Requirements 5.1
     */
    @Test
    void ipKeyResolver_differentIps_produceDifferentKeys() {
        MockServerWebExchange exchange1 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login")
                        .remoteAddress(new InetSocketAddress("10.0.0.1", 1000))
                        .build());
        MockServerWebExchange exchange2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login")
                        .remoteAddress(new InetSocketAddress("10.0.0.2", 1000))
                        .build());

        String key1 = ipKeyResolver.resolve(exchange1).block();
        String key2 = ipKeyResolver.resolve(exchange2).block();

        assertThat(key1).isNotEqualTo(key2);
    }

    // -------------------------------------------------------------------------
    // userKeyResolver tests
    // -------------------------------------------------------------------------

    /**
     * userKeyResolver must return the X-User-Id header value when present.
     *
     * Validates: Requirements 5.2 — rate limiting per user for protected routes
     */
    @Test
    void userKeyResolver_withXUserIdHeader_returnsUserId() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/accounts/123")
                .header("X-User-Id", "user-42")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = userKeyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("user-42");
    }

    /**
     * userKeyResolver must fall back to "anonymous" when X-User-Id header is absent.
     *
     * Validates: Requirements 5.2 — anonymous fallback prevents rate limiter from blocking
     * on missing key
     */
    @Test
    void userKeyResolver_withoutXUserIdHeader_returnsAnonymous() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/accounts/456")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = userKeyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("anonymous");
    }

    /**
     * userKeyResolver must return the exact header value — different users produce different keys.
     *
     * Validates: Requirements 5.2
     */
    @Test
    void userKeyResolver_differentUsers_produceDifferentKeys() {
        MockServerWebExchange exchange1 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/transfers/1")
                        .header("X-User-Id", "user-100")
                        .build());
        MockServerWebExchange exchange2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/transfers/2")
                        .header("X-User-Id", "user-200")
                        .build());

        String key1 = userKeyResolver.resolve(exchange1).block();
        String key2 = userKeyResolver.resolve(exchange2).block();

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).isEqualTo("user-100");
        assertThat(key2).isEqualTo("user-200");
    }

    /**
     * userKeyResolver returns the header value as-is when X-User-Id is present (even if empty).
     * The "anonymous" fallback only applies when the header is completely absent (null).
     *
     * Validates: Requirements 5.2
     */
    @Test
    void userKeyResolver_withEmptyXUserIdHeader_returnsEmptyString() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/transactions/history")
                .header("X-User-Id", "")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = userKeyResolver.resolve(exchange).block();

        // Empty string header is present (not null), so justOrEmpty emits it as-is.
        // defaultIfEmpty only triggers when the Mono is empty (header absent / null).
        assertThat(key).isEqualTo("");
    }

    // -------------------------------------------------------------------------
    // Bean configuration tests
    // -------------------------------------------------------------------------

    /**
     * ipKeyResolver bean must be non-null (Spring context wires it correctly).
     *
     * Validates: Requirements 5.5 — Redis-backed RequestRateLimiter requires a KeyResolver bean
     */
    @Test
    void ipKeyResolver_beanIsNotNull() {
        assertThat(ipKeyResolver).isNotNull();
    }

    /**
     * userKeyResolver bean must be non-null.
     *
     * Validates: Requirements 5.5
     */
    @Test
    void userKeyResolver_beanIsNotNull() {
        assertThat(userKeyResolver).isNotNull();
    }

    /**
     * ipKeyResolver and userKeyResolver must be distinct beans.
     *
     * Validates: Requirements 5.1, 5.2 — two separate rate limiting strategies
     */
    @Test
    void ipKeyResolver_andUserKeyResolver_areDistinctBeans() {
        assertThat(ipKeyResolver).isNotSameAs(userKeyResolver);
    }
}
