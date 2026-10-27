package com.example.recruitment.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class TokenUtils {

	private static final SecureRandom RANDOM = new SecureRandom();

	private TokenUtils() {
	}

	/** Sinh chuỗi ngẫu nhiên an toàn, dùng làm refresh token hoặc token gửi qua email. */
	public static String generateToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Băm SHA-256 để lưu vào CSDL, không lưu token gốc. */
	public static String sha256Hex(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Thuật toán SHA-256 không khả dụng", e);
		}
	}
}
