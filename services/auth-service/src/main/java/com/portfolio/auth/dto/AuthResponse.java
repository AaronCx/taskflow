package com.portfolio.auth.dto;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String firstName,
        String lastName
) {}
