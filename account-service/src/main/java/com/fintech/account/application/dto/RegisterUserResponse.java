package com.fintech.account.application.dto;

import java.time.Instant;

public record RegisterUserResponse(String userId, String email, Instant createdAt) {}
