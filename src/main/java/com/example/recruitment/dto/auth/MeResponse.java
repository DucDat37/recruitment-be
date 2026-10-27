package com.example.recruitment.dto.auth;

import com.example.recruitment.entity.enums.UserRole;

public record MeResponse(
		Long id,
		String email,
		UserRole role,
		boolean emailVerified
) {
}
