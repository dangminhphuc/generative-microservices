package com.fintech.account.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(@NotBlank String refreshToken) {}
