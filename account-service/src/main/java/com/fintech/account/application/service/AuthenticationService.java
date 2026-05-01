package com.fintech.account.application.service;

import com.fintech.account.application.dto.LoginCommand;
import com.fintech.account.application.dto.LoginResponse;
import com.fintech.account.application.dto.RefreshTokenCommand;
import com.fintech.account.domain.model.Email;
import com.fintech.account.domain.model.User;
import com.fintech.account.domain.port.in.LoginUseCase;
import com.fintech.account.domain.port.in.RefreshTokenUseCase;
import com.fintech.account.domain.port.out.PasswordEncoder;
import com.fintech.account.domain.port.out.UserRepository;
import com.fintech.account.infrastructure.security.JwtTokenService;
import com.fintech.common.exception.BusinessRuleException;
import com.fintech.common.exception.ResourceNotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthenticationService implements LoginUseCase, RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenService jwtTokenService,
                                 MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.loginSuccessCounter = Counter.builder("auth.login.success").register(meterRegistry);
        this.loginFailureCounter = Counter.builder("auth.login.failure").register(meterRegistry);
    }

    @Override
    @Transactional
    public LoginResponse execute(LoginCommand command) {
        Email email = Email.of(command.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException(
                        "Email hoặc mật khẩu không đúng", "INVALID_CREDENTIALS"));

        if (user.isLocked()) {
            log.warn("Login attempt on locked account userId={}", user.getId());
            loginFailureCounter.increment();
            throw new BusinessRuleException("Tài khoản tạm khóa", "ACCOUNT_LOCKED");
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            log.warn("Login failed for userId={}", user.getId());
            loginFailureCounter.increment();
            throw new BusinessRuleException("Email hoặc mật khẩu không đúng", "INVALID_CREDENTIALS");
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);
        log.info("Login successful userId={}", user.getId());
        loginSuccessCounter.increment();

        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken, jwtTokenService.getAccessTokenExpiry());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse execute(RefreshTokenCommand command) {
        UUID userId = jwtTokenService.validateRefreshToken(command.refreshToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken, jwtTokenService.getAccessTokenExpiry());
    }
}
