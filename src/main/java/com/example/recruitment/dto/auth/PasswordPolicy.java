package com.example.recruitment.dto.auth;

/** Quy tắc mật khẩu: 8 đến 72 ký tự, gồm ít nhất một chữ và một số (72 là giới hạn của BCrypt). */
public final class PasswordPolicy {

	public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
	public static final String MESSAGE = "Mật khẩu từ 8 đến 72 ký tự, gồm ít nhất một chữ và một số";

	private PasswordPolicy() {
	}
}
