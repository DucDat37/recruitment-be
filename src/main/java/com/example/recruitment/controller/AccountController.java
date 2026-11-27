package com.example.recruitment.controller;

import com.example.recruitment.common.ApiResponse;
import com.example.recruitment.dto.auth.ChangePasswordRequest;
import com.example.recruitment.dto.auth.MeResponse;
import com.example.recruitment.security.AuthUser;
import com.example.recruitment.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@GetMapping
	public ApiResponse<MeResponse> getMe(@AuthenticationPrincipal AuthUser principal) {
		return ApiResponse.success(accountService.getMe(principal.id()), "Lấy thông tin tài khoản thành công.");
	}

	@PostMapping("/password")
	public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthUser principal,
											@Valid @RequestBody ChangePasswordRequest request) {
		accountService.changePassword(principal.id(), request);
		return ApiResponse.success("Đổi mật khẩu thành công. Các phiên đăng nhập khác đã bị đăng xuất.");
	}

	@PostMapping("/logout-all")
	public ApiResponse<Void> logoutAll(@AuthenticationPrincipal AuthUser principal) {
		accountService.logoutAll(principal.id());
		return ApiResponse.success("Đã đăng xuất khỏi tất cả thiết bị.");
	}
}
