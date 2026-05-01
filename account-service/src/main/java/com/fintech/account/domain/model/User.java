package com.fintech.account.domain.model;

import com.fintech.common.domain.BaseEntity;
import java.time.Instant;
import java.util.UUID;

public class User extends BaseEntity {

    private Email email;
    private String passwordHash;
    private String fullName;
    private PhoneNumber phoneNumber;
    private UserStatus status;
    private int failedLoginAttempts;
    private Instant lockedUntil;

    private User() { super(); }

    public static User create(Email email, String passwordHash, String fullName, PhoneNumber phoneNumber) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        user.phoneNumber = phoneNumber;
        user.status = UserStatus.ACTIVE;
        user.failedLoginAttempts = 0;
        user.lockedUntil = null;
        return user;
    }

    public static User reconstitute(UUID id, Email email, String passwordHash, String fullName,
                                     PhoneNumber phoneNumber, UserStatus status,
                                     int failedLoginAttempts, Instant lockedUntil,
                                     Instant createdAt, Instant updatedAt) {
        User user = new User();
        // Use reflection-free reconstitution via protected setters
        user.email = email;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        user.phoneNumber = phoneNumber;
        user.status = status;
        user.failedLoginAttempts = failedLoginAttempts;
        user.lockedUntil = lockedUntil;
        return user;
    }

    public boolean isLocked() {
        return status == UserStatus.LOCKED && lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.status = UserStatus.LOCKED;
            this.lockedUntil = Instant.now().plusSeconds(900); // 15 minutes
        }
        markUpdated();
    }

    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        if (this.status == UserStatus.LOCKED && !isLocked()) {
            this.status = UserStatus.ACTIVE;
        }
        markUpdated();
    }

    public Email getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public PhoneNumber getPhoneNumber() { return phoneNumber; }
    public UserStatus getStatus() { return status; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
}
