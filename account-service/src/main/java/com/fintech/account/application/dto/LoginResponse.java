package com.fintech.account.application.dto;

public record LoginResponse(String accessToken, String refreshToken, long expiresIn) {}
