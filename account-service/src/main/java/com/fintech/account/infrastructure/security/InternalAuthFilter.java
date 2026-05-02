package com.fintech.account.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that protects /internal/** endpoints by requiring a valid X-Internal-Secret header.
 * This provides service-to-service authentication so that internal APIs cannot be accessed
 * even if the load balancer or VPC is misconfigured.
 *
 * - Missing or empty header → 401 Unauthorized
 * - Wrong secret value → 403 Forbidden
 * - Correct secret → request proceeds normally
 */
@Component
@Order(1)
public class InternalAuthFilter extends OncePerRequestFilter {

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/internal/")) {
            String secret = request.getHeader("X-Internal-Secret");
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
