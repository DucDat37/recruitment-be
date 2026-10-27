package com.example.recruitment.dto.admin;

import com.example.recruitment.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
		@NotNull UserStatus status
) {
}
