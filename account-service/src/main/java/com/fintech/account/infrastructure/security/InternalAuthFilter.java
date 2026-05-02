package com.fintech.account.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that protects /internal/** endpoints by requiring a valid X-Internal-Secret header.
 * Runs before Spring Security's FilterChainProxy (order HIGHEST_PRECEDENCE + 2) so it can
 * short-circuit with 401/403 before Spring Security processes the request.
 *
 * <ul>
 *   <li>No header or empty string → HTTP 401 Unauthorized</li>
 *   <li>Wrong secret value → HTTP 403 Forbidden</li>
 *   <li>Correct secret → proceed with filter chain</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class InternalAuthFilter extends OncePerRequestFilter {

    static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    static final String INTERNAL_PATH_PREFIX = "/internal/";

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            String secret = request.getHeader(INTERNAL_SECRET_HEADER);

            if (secret == null || secret.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (!internalApiSecret.equals(secret)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
