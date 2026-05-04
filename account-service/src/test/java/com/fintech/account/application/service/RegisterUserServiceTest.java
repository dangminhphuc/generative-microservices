package com.fintech.account.application.service;

import com.fintech.account.application.dto.RegisterUserCommand;
import com.fintech.account.application.dto.RegisterUserResponse;
import com.fintech.account.domain.model.Email;
import com.fintech.account.domain.model.PhoneNumber;
import com.fintech.account.domain.model.User;
import com.fintech.account.domain.port.out.PasswordEncoder;
import com.fintech.account.domain.port.out.UserRepository;
import com.fintech.common.exception.BusinessRuleException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisterUserService registerUserService;

    private static final String TEST_EMAIL = "newuser@example.com";
    private static final String TEST_PHONE = "0901234567";
    private static final String TEST_PASSWORD = "P@ssw0rd!";
    private static final String TEST_FULL_NAME = "Nguyen Van A";
    private static final String HASHED_PASSWORD = "hashed_password";

    @BeforeEach
    void setUp() {
        registerUserService = new RegisterUserService(
                userRepository,
                passwordEncoder,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void register_withValidData_returnsResponse() {
        // Arrange
        when(userRepository.existsByEmail(Email.of(TEST_EMAIL))).thenReturn(false);
        when(userRepository.existsByPhoneNumber(PhoneNumber.of(TEST_PHONE))).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterUserCommand command = new RegisterUserCommand(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME, TEST_PHONE);

        // Act
        RegisterUserResponse response = registerUserService.execute(command);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.userId()).isNotNull();
        assertThat(response.email()).isEqualTo(TEST_EMAIL);
    }

    @Test
    void register_withDuplicateEmail_throwsEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(Email.of(TEST_EMAIL))).thenReturn(true);

        RegisterUserCommand command = new RegisterUserCommand(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME, TEST_PHONE);

        // Act & Assert
        assertThatThrownBy(() -> registerUserService.execute(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", "EMAIL_ALREADY_EXISTS");
    }

    @Test
    void register_withDuplicatePhone_throwsPhoneAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(Email.of(TEST_EMAIL))).thenReturn(false);
        when(userRepository.existsByPhoneNumber(PhoneNumber.of(TEST_PHONE))).thenReturn(true);

        RegisterUserCommand command = new RegisterUserCommand(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME, TEST_PHONE);

        // Act & Assert
        assertThatThrownBy(() -> registerUserService.execute(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PHONE_ALREADY_EXISTS");
    }

    @Test
    void register_passwordIsHashed() {
        // Arrange
        when(userRepository.existsByEmail(Email.of(TEST_EMAIL))).thenReturn(false);
        when(userRepository.existsByPhoneNumber(PhoneNumber.of(TEST_PHONE))).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        RegisterUserCommand command = new RegisterUserCommand(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME, TEST_PHONE);

        // Act
        registerUserService.execute(command);

        // Assert — passwordEncoder.encode() was called with the raw password
        verify(passwordEncoder).encode(TEST_PASSWORD);

        // Assert — save() received a user whose passwordHash is NOT the raw password
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(TEST_PASSWORD);
        assertThat(savedUser.getPasswordHash()).isEqualTo(HASHED_PASSWORD);
    }
}
