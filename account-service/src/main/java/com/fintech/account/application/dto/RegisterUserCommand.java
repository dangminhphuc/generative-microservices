package com.fintech.account.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserCommand(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must contain uppercase, lowercase, digit, and special character")
        String password,
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Pattern(regexp = "^0\\d{9}$", message = "Invalid VN phone number") String phoneNumber
) {}
