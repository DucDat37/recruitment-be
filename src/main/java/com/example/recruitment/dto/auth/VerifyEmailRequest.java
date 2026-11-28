package com.example.recruitment.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
		@NotBlank @Email String email,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "Mã xác thực phải gồm 6 chữ số") String code
) {
}
