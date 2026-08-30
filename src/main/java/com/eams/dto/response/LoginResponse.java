package com.eams.dto.response;

public record LoginResponse(
        String token,
        String email,
        String role
) {
}