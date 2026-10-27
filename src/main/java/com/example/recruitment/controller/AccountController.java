package com.example.recruitment.controller;

import com.example.recruitment.dto.auth.ChangePasswordRequest;
import com.example.recruitment.dto.auth.MeResponse;
import com.example.recruitment.dto.common.MessageResponse;
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
	public MeResponse getMe(@AuthenticationPrincipal AuthUser principal) {
		return accountService.getMe(principal.id());
	}

	@PostMapping("/password")
	public MessageResponse changePassword(@AuthenticationPrincipal AuthUser principal,
										  @Valid @RequestBody ChangePasswordRequest request) {
		accountService.changePassword(principal.id(), request);
		return new MessageResponse("Đổi mật khẩu thành công. Các phiên đăng nhập khác đã bị đăng xuất.");
	}

	@PostMapping("/logout-all")
	public MessageResponse logoutAll(@AuthenticationPrincipal AuthUser principal) {
		accountService.logoutAll(principal.id());
		return new MessageResponse("Đã đăng xuất khỏi tất cả thiết bị.");
	}
}
