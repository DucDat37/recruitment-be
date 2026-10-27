package com.example.recruitment.service;

import com.example.recruitment.entity.User;
import com.example.recruitment.entity.enums.UserStatus;
import com.example.recruitment.exception.ApiException;
import com.example.recruitment.repository.RefreshTokenRepository;
import com.example.recruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminUserService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	/** Khi khóa hoặc vô hiệu hóa, thu hồi toàn bộ refresh token để không gia hạn được phiên. */
	@Transactional
	public void updateStatus(Long userId, UserStatus status) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"));
		user.setStatus(status);
		if (status != UserStatus.ACTIVE) {
			refreshTokenRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());
		}
	}
}
