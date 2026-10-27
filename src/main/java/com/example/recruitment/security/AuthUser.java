package com.example.recruitment.security;

import com.example.recruitment.entity.enums.UserRole;

/** Thông tin người dùng đã xác thực, được gắn vào SecurityContext cho mỗi request. */
public record AuthUser(Long id, UserRole role) {
}
