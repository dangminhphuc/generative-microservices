package com.fintech.account.application.service;

import com.fintech.account.application.dto.LoginCommand;
import com.fintech.account.application.dto.LoginResponse;
import com.fintech.account.application.dto.RefreshTokenCommand;
import com.fintech.account.domain.model.Email;
import com.fintech.account.domain.model.PhoneNumber;
import com.fintech.account.domain.model.User;
import com.fintech.account.domain.port.out.PasswordEncoder;
import com.fintech.account.domain.port.out.UserRepository;
import com.fintech.account.infrastructure.security.JwtTokenService;
import com.fintech.common.exception.BusinessRuleException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthenticationService authenticationService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_HASH = "hashed_password";

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository,
                passwordEncoder,
                jwtTokenService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void login_withValidCredentials_returnsTokens() {
        // Arrange
        User user = User.create(
                Email.of(TEST_EMAIL),
                TEST_HASH,
                "Test User",
                PhoneNumber.of("0901234567")
        );
        when(userRepository.findByEmail(Email.of(TEST_EMAIL))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_HASH)).thenReturn(true);
        when(jwtTokenService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtTokenService.getAccessTokenExpiry()).thenReturn(3600000L);

        // Act
        LoginResponse response = authenticationService.execute(new LoginCommand(TEST_EMAIL, TEST_PASSWORD));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        // Arrange
        User user = User.create(
                Email.of(TEST_EMAIL),
                TEST_HASH,
                "Test User",
                PhoneNumber.of("0901234567")
        );
        when(userRepository.findByEmail(Email.of(TEST_EMAIL))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_HASH)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.execute(new LoginCommand(TEST_EMAIL, TEST_PASSWORD)))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_CREDENTIALS");

        verify(userRepository).save(user);
    }

    @Test
    void login_withLockedAccount_throwsAccountLocked() {
        // Arrange — create a user and lock it by calling recordFailedLogin 5 times
        User user = User.create(
                Email.of(TEST_EMAIL),
                TEST_HASH,
                "Test User",
                PhoneNumber.of("0901234567")
        );
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin();
        }
        when(userRepository.findByEmail(Email.of(TEST_EMAIL))).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.execute(new LoginCommand(TEST_EMAIL, TEST_PASSWORD)))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_LOCKED");
    }

    @Test
    void login_afterFiveFailures_accountBecomesLocked() {
        // Arrange
        User user = User.create(
                Email.of(TEST_EMAIL),
                TEST_HASH,
                "Test User",
                PhoneNumber.of("0901234567")
        );

        // Act — call recordFailedLogin 5 times
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin();
        }

        // Assert
        assertThat(user.isLocked()).isTrue();
    }

    @Test
    void refreshToken_withValidToken_returnsNewTokens() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = User.create(
                Email.of(TEST_EMAIL),
                TEST_HASH,
                "Test User",
                PhoneNumber.of("0901234567")
        );
        String refreshToken = "valid-refresh-token";

        when(jwtTokenService.validateRefreshToken(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtTokenService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        when(jwtTokenService.getAccessTokenExpiry()).thenReturn(3600000L);

        // Act
        LoginResponse response = authenticationService.execute(new RefreshTokenCommand(refreshToken));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
    }

    @Test
    void refreshToken_withExpiredToken_throwsTokenExpired() {
        // Arrange
        String expiredToken = "expired-refresh-token";
        when(jwtTokenService.validateRefreshToken(expiredToken))
                .thenThrow(new BusinessRuleException("Token đã hết hạn", "TOKEN_EXPIRED"));

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.execute(new RefreshTokenCommand(expiredToken)))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", "TOKEN_EXPIRED");
    }
}
