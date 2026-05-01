package com.fintech.account.application.service;

import com.fintech.account.application.dto.RegisterUserCommand;
import com.fintech.account.application.dto.RegisterUserResponse;
import com.fintech.account.domain.model.Email;
import com.fintech.account.domain.model.PhoneNumber;
import com.fintech.account.domain.model.User;
import com.fintech.account.domain.port.in.RegisterUserUseCase;
import com.fintech.account.domain.port.out.PasswordEncoder;
import com.fintech.account.domain.port.out.UserRepository;
import com.fintech.common.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Counter registerSuccessCounter;

    public RegisterUserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                               MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registerSuccessCounter = Counter.builder("auth.register.success").register(meterRegistry);
    }

    @Override
    @Transactional
    public RegisterUserResponse execute(RegisterUserCommand command) {
        Email email = Email.of(command.email());
        PhoneNumber phoneNumber = PhoneNumber.of(command.phoneNumber());

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected: email already exists");
            throw new BusinessRuleException("Email đã được sử dụng", "EMAIL_ALREADY_EXISTS");
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            log.warn("Registration rejected: phone number already exists");
            throw new BusinessRuleException("Số điện thoại đã được sử dụng", "PHONE_ALREADY_EXISTS");
        }

        String hashedPassword = passwordEncoder.encode(command.password());
        User user = User.create(email, hashedPassword, command.fullName(), phoneNumber);
        User saved = userRepository.save(user);

        log.info("User registered userId={}", saved.getId());
        registerSuccessCounter.increment();

        return new RegisterUserResponse(
                saved.getId().toString(),
                saved.getEmail().getValue(),
                saved.getCreatedAt()
        );
    }
}
