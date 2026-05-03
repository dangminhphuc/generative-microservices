package com.fintech.account.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(0)
public class GatewaySecretFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewaySecretFilter.class);

    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/actuator/health",
            "/actuator/info"
    );

    @Value("${internal.api.secret}")
    private String expectedSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Allow actuator health/info without secret (for Docker healthcheck)
        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        String secret = request.getHeader(GATEWAY_SECRET_HEADER);
        if (secret == null || secret.isEmpty()) {
            log.warn("Request to {} rejected: missing or empty X-Gateway-Secret header", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (!expectedSecret.equals(secret)) {
            log.warn("Request to {} rejected: invalid X-Gateway-Secret header", path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }
}
