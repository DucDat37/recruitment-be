package com.example.recruitment.controller;

import com.example.recruitment.common.ApiResponse;
import com.example.recruitment.dto.auth.EmailRequest;
import com.example.recruitment.dto.auth.LoginRequest;
import com.example.recruitment.dto.auth.RefreshTokenRequest;
import com.example.recruitment.dto.auth.RegisterRequest;
import com.example.recruitment.dto.auth.ResetPasswordRequest;
import com.example.recruitment.dto.auth.TokenResponse;
import com.example.recruitment.dto.auth.VerifyEmailRequest;
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
	public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
		authService.register(request);
		return ApiResponse.success("Đăng ký thành công. Vui lòng kiểm tra email để lấy mã xác thực tài khoản.");
	}

	@PostMapping("/verify-email")
	public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		authService.verifyEmail(request.email(), request.code());
		return ApiResponse.success("Xác thực email thành công.");
	}

	@PostMapping("/resend-verification")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ApiResponse<Void> resendVerification(@Valid @RequestBody EmailRequest request) {
		authService.resendVerification(request.email());
		return ApiResponse.success("Nếu email tồn tại và chưa xác thực, chúng tôi đã gửi lại mã xác thực.");
	}

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request), "Đăng nhập thành công.");
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request.refreshToken()), "Làm mới token thành công.");
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request.refreshToken());
		return ApiResponse.success("Đăng xuất thành công.");
	}

	@PostMapping("/forgot-password")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ApiResponse<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
		authService.forgotPassword(request.email());
		return ApiResponse.success("Nếu email tồn tại, chúng tôi đã gửi link đặt lại mật khẩu.");
	}

	@PostMapping("/reset-password")
	public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request.token(), request.newPassword());
		return ApiResponse.success("Đặt lại mật khẩu thành công.");
	}
}
