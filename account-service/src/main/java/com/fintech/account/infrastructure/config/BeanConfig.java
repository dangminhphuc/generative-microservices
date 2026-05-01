package com.fintech.account.infrastructure.config;

import com.fintech.account.domain.port.out.BankAccountRepository;
import com.fintech.account.domain.port.out.PasswordEncoder;
import com.fintech.account.domain.service.AccountNumberGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class BeanConfig {

    @Bean
    public AccountNumberGenerator accountNumberGenerator(BankAccountRepository bankAccountRepository) {
        return new AccountNumberGenerator(bankAccountRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        return new PasswordEncoder() {
            @Override
            public String encode(String rawPassword) {
                return bcrypt.encode(rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return bcrypt.matches(rawPassword, encodedPassword);
            }
        };
    }
}
