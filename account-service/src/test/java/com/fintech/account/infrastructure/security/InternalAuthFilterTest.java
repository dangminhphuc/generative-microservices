package com.fintech.account.infrastructure.security;

import com.fintech.account.domain.model.AccountNumber;
import com.fintech.account.domain.port.in.GetAccountByNumberUseCase;
import com.fintech.account.infrastructure.adapter.in.rest.InternalAccountController;
import com.fintech.common.dto.AccountInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for InternalAuthFilter — verifies X-Internal-Secret header enforcement
 * on /internal/** endpoints.
 *
 * Validates: Requirement 2 (Internal Endpoints Protection)
 */
@WebMvcTest(controllers = InternalAccountController.class)
@Import({InternalAuthFilter.class, SecurityConfig.class})
@TestPropertySource(properties = "internal.api.secret=test-secret-value")
class InternalAuthFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetAccountByNumberUseCase getAccountByNumberUseCase;

    @Test
    @DisplayName("1.2.6 - Request without X-Internal-Secret header → 401 Unauthorized")
    void requestWithoutHeader_returns401() throws Exception {
        mockMvc.perform(get("/internal/accounts/1234567890"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("1.2.7 - Request with wrong X-Internal-Secret header → 403 Forbidden")
    void requestWithWrongSecret_returns403() throws Exception {
        mockMvc.perform(get("/internal/accounts/1234567890")
                        .header("X-Internal-Secret", "wrong-secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("1.2.8 - Request with correct X-Internal-Secret header → 200 OK")
    void requestWithCorrectSecret_returns200() throws Exception {
        AccountInfoResponse mockResponse = new AccountInfoResponse(
                "acc-uuid-1", "1234567890", "John Doe", "ACTIVE"
        );
        when(getAccountByNumberUseCase.execute(any(AccountNumber.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/internal/accounts/1234567890")
                        .header("X-Internal-Secret", "test-secret-value"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("1.2.9 - Request with empty X-Internal-Secret header → 401 Unauthorized")
    void requestWithEmptyHeader_returns401() throws Exception {
        mockMvc.perform(get("/internal/accounts/1234567890")
                        .header("X-Internal-Secret", ""))
                .andExpect(status().isUnauthorized());
    }
}
