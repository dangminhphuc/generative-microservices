package com.fintech.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Rate limiter configuration for API Gateway.
 *
 * <p>Provides two {@link KeyResolver} beans:
 * <ul>
 *   <li>{@code ipKeyResolver} (primary) — resolves the client's remote IP address.
 *       Used for unauthenticated auth routes where no user context is available.</li>
 *   <li>{@code userKeyResolver} — resolves the {@code X-User-Id} header injected by
 *       {@code JwtAuthenticationFilter}. Falls back to {@code "anonymous"} when the
 *       header is absent (e.g., public paths).</li>
 * </ul>
 *
 * <p>Rate limits (token bucket via Redis):
 * <ul>
 *   <li>Auth routes ({@code /api/auth/**}): 10 req/min per IP</li>
 *   <li>Protected routes: 100 req/min per user</li>
 * </ul>
 */
@Configuration
public class RateLimiterConfig {

    /**
     * IP-based key resolver — primary bean used by default when no explicit
     * {@code key-resolver} SpEL is specified.
     *
     * <p>Used for auth routes ({@code /api/auth/**}) where requests are unauthenticated
     * and rate limiting must be enforced per client IP to prevent brute-force attacks.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress().getHostAddress());
    }

    /**
     * User-based key resolver — resolves the {@code X-User-Id} header.
     *
     * <p>Used for protected routes where the JWT filter has already validated the token
     * and injected the user identifier. Falls back to {@code "anonymous"} when the header
     * is absent so the rate limiter never blocks on a missing key.
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                        exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .defaultIfEmpty("anonymous");
    }
}
