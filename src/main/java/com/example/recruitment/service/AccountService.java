package com.example.recruitment.service;

import com.example.recruitment.dto.auth.ChangePasswordRequest;
import com.example.recruitment.dto.auth.MeResponse;
import com.example.recruitment.entity.User;
import com.example.recruitment.exception.ApiException;
import com.example.recruitment.repository.RefreshTokenRepository;
import com.example.recruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Nghiệp vụ dành cho người dùng đã đăng nhập: xem thông tin, đổi mật khẩu, đăng xuất khỏi mọi thiết bị. */
@Service
@RequiredArgsConstructor
public class AccountService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional(readOnly = true)
	public MeResponse getMe(Long userId) {
		User user = findUser(userId);
		return new MeResponse(user.getId(), user.getEmail(), user.getRole(), user.getEmailVerifiedAt() != null);
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		User user = findUser(userId);
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_CURRENT_PASSWORD", "Mật khẩu hiện tại không đúng");
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());
	}

	@Transactional
	public void logoutAll(Long userId) {
		refreshTokenRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Tài khoản không tồn tại"));
	}
}
