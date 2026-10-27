package com.example.recruitment.controller;

import com.example.recruitment.dto.auth.EmailRequest;
import com.example.recruitment.dto.auth.LoginRequest;
import com.example.recruitment.dto.auth.RefreshTokenRequest;
import com.example.recruitment.dto.auth.RegisterRequest;
import com.example.recruitment.dto.auth.ResetPasswordRequest;
import com.example.recruitment.dto.auth.TokenResponse;
import com.example.recruitment.dto.auth.VerifyEmailRequest;
import com.example.recruitment.dto.common.MessageResponse;
import com.example.recruitment.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
		authService.register(request);
		return new MessageResponse("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.");
	}

	@PostMapping("/verify-email")
	public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		authService.verifyEmail(request.token());
		return new MessageResponse("Xác thực email thành công.");
	}

	@PostMapping("/resend-verification")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public MessageResponse resendVerification(@Valid @RequestBody EmailRequest request) {
		authService.resendVerification(request.email());
		return new MessageResponse("Nếu email tồn tại và chưa xác thực, chúng tôi đã gửi lại link xác thực.");
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return authService.refresh(request.refreshToken());
	}

	@PostMapping("/logout")
	public MessageResponse logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request.refreshToken());
		return new MessageResponse("Đăng xuất thành công.");
	}

	@PostMapping("/forgot-password")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
		authService.forgotPassword(request.email());
		return new MessageResponse("Nếu email tồn tại, chúng tôi đã gửi link đặt lại mật khẩu.");
	}

	@PostMapping("/reset-password")
	public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request.token(), request.newPassword());
		return new MessageResponse("Đặt lại mật khẩu thành công.");
	}
}
