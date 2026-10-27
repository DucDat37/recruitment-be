package com.example.recruitment.controller;

import com.example.recruitment.dto.admin.UpdateUserStatusRequest;
import com.example.recruitment.dto.common.MessageResponse;
import com.example.recruitment.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final AdminUserService adminUserService;

	@PatchMapping("/{userId}/status")
	public MessageResponse updateStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request) {
		adminUserService.updateStatus(userId, request.status());
		return new MessageResponse("Cập nhật trạng thái tài khoản thành công.");
	}
}
