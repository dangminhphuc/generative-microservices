package com.fintech.account.domain.port.out;

import com.fintech.account.domain.model.Email;
import com.fintech.account.domain.model.PhoneNumber;
import com.fintech.account.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(Email email);
    boolean existsByEmail(Email email);
    boolean existsByPhoneNumber(PhoneNumber phoneNumber);
}
