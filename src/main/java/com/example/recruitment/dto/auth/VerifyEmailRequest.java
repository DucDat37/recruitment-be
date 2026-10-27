package com.example.recruitment.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
		@NotBlank String token
) {
}
