package com.example.recruitment.dto.auth;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn
) {
}
