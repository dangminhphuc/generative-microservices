package com.fintech.account.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InternalAuthFilter}.
 *
 * Covers correctness properties from Requirement 2:
 *
 * <ol>
 *   <li>Request to /internal/** without X-Internal-Secret header → 401 Unauthorized</li>
 *   <li>Request to /internal/** with wrong secret → 403 Forbidden</li>
 *   <li>Request to /internal/** with correct secret → passes through (200 from downstream)</li>
 *   <li>Request to /internal/** with empty header "" → 401 Unauthorized (treated as missing)</li>
 * </ol>
 */
class InternalAuthFilterTest {

    private static final String CORRECT_SECRET = "test-internal-secret-value";
    private static final String WRONG_SECRET = "wrong-secret";

    private InternalAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalAuthFilter();
        // Inject the secret value directly (simulates @Value injection)
        ReflectionTestUtils.setField(filter, "internalApiSecret", CORRECT_SECRET);
    }

    // -----------------------------------------------------------------------
    // Task 1.2.6 — no header → 401
    // -----------------------------------------------------------------------

    /**
     * Verifies Acceptance Criterion 1:
     * WHEN a request to /internal/** has no X-Internal-Secret header,
     * THEN the filter SHALL return 401 Unauthorized.
     */
    @Test
    void internalPath_noHeader_returns401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/ACC123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        // Chain must NOT have been called — request was rejected
        assertThat(chain.getRequest()).isNull();
    }

    // -----------------------------------------------------------------------
    // Task 1.2.7 — wrong header → 403
    // -----------------------------------------------------------------------

    /**
     * Verifies Acceptance Criterion 2:
     * WHEN a request to /internal/** has X-Internal-Secret with a wrong value,
     * THEN the filter SHALL return 403 Forbidden.
     */
    @Test
    void internalPath_wrongSecret_returns403() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/ACC123");
        request.addHeader("X-Internal-Secret", WRONG_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        // Chain must NOT have been called — request was rejected
        assertThat(chain.getRequest()).isNull();
    }

    // -----------------------------------------------------------------------
    // Task 1.2.8 — correct header → passes through (200)
    // -----------------------------------------------------------------------

    /**
     * Verifies Acceptance Criterion 3:
     * WHEN a request to /internal/** has the correct X-Internal-Secret,
     * THEN the filter SHALL pass the request to the next filter in the chain.
     */
    @Test
    void internalPath_correctSecret_passesThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/ACC123");
        request.addHeader("X-Internal-Secret", CORRECT_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        // Chain was called — request was forwarded
        assertThat(chain.getRequest()).isNotNull();
        // Default MockHttpServletResponse status is 200
        assertThat(response.getStatus()).isEqualTo(200);
    }

    // -----------------------------------------------------------------------
    // Task 1.2.9 — empty header "" → 401
    // -----------------------------------------------------------------------

    /**
     * Verifies Error Condition from Requirement 2:
     * WHEN a request to /internal/** has X-Internal-Secret set to empty string "",
     * THEN the filter SHALL return 401 Unauthorized (treated as missing).
     */
    @Test
    void internalPath_emptyHeader_returns401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/ACC123");
        request.addHeader("X-Internal-Secret", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        // Chain must NOT have been called — request was rejected
        assertThat(chain.getRequest()).isNull();
    }

    // -----------------------------------------------------------------------
    // Additional: non-internal paths are NOT affected by the filter
    // -----------------------------------------------------------------------

    /**
     * Verifies that non-internal paths pass through the filter without any secret check.
     */
    @Test
    void nonInternalPath_noHeader_passesThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        // Chain was called — no secret check for non-internal paths
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
