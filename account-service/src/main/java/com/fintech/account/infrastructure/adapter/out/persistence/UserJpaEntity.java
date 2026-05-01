package com.fintech.account.infrastructure.adapter.out.persistence;

import com.fintech.account.domain.model.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {}

    public static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.getId();
        entity.email = user.getEmail().getValue();
        entity.passwordHash = user.getPasswordHash();
        entity.fullName = user.getFullName();
        entity.phoneNumber = user.getPhoneNumber().getValue();
        entity.status = user.getStatus();
        entity.failedLoginAttempts = user.getFailedLoginAttempts();
        entity.lockedUntil = user.getLockedUntil();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }

    public User toDomain() {
        return User.reconstitute(id, Email.of(email), passwordHash, fullName,
                PhoneNumber.of(phoneNumber), status, failedLoginAttempts, lockedUntil,
                createdAt, updatedAt);
    }

    // Getters for JPA queries
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
}
