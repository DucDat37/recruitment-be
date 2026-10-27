package com.example.recruitment.service;

import com.example.recruitment.common.TokenUtils;
import com.example.recruitment.config.AppProperties;
import com.example.recruitment.dto.auth.LoginRequest;
import com.example.recruitment.dto.auth.RegisterRequest;
import com.example.recruitment.dto.auth.TokenResponse;
import com.example.recruitment.entity.AccountToken;
import com.example.recruitment.entity.RefreshToken;
import com.example.recruitment.entity.User;
import com.example.recruitment.entity.enums.AccountTokenPurpose;
import com.example.recruitment.entity.enums.UserRole;
import com.example.recruitment.entity.enums.UserStatus;
import com.example.recruitment.exception.ApiException;
import com.example.recruitment.repository.AccountTokenRepository;
import com.example.recruitment.repository.RefreshTokenRepository;
import com.example.recruitment.repository.UserRepository;
import com.example.recruitment.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Luồng xác thực công khai: đăng ký, xác thực email, đăng nhập, làm mới token,
 * đăng xuất và đặt lại mật khẩu.
 *
 * Các method có {@code noRollbackFor = ApiException.class} cần giữ lại thay đổi trạng thái
 * trước khi ném lỗi (đếm đăng nhập sai, thu hồi phiên khi phát hiện token bị dùng lại).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String INVALID_CREDENTIALS = "Email hoặc mật khẩu không đúng";

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AccountTokenRepository accountTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final EmailSender emailSender;
	private final AppProperties properties;

	@Transactional
	public void register(RegisterRequest request) {
		if (request.role() == UserRole.ADMIN) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Không thể đăng ký tài khoản quản trị");
		}
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email đã được sử dụng");
		}
		User user = userRepository.save(
				new User(email, passwordEncoder.encode(request.password()), request.role()));
		String token = issueAccountToken(user, AccountTokenPurpose.EMAIL_VERIFY,
				Duration.ofHours(properties.auth().verifyTtlHours()));
		sendVerificationEmail(user, token);
	}

	@Transactional
	public void verifyEmail(String rawToken) {
		AccountToken token = findUsableToken(rawToken, AccountTokenPurpose.EMAIL_VERIFY);
		LocalDateTime now = LocalDateTime.now();
		token.setUsedAt(now);
		token.getUser().setEmailVerifiedAt(now);
	}

	/** Luôn kết thúc êm, kể cả email không tồn tại, để không lộ thông tin tài khoản. */
	@Transactional
	public void resendVerification(String email) {
		userRepository.findByEmail(normalizeEmail(email))
				.filter(user -> user.getEmailVerifiedAt() == null)
				.ifPresent(user -> {
					String token = issueAccountToken(user, AccountTokenPurpose.EMAIL_VERIFY,
							Duration.ofHours(properties.auth().verifyTtlHours()));
					sendVerificationEmail(user, token);
				});
	}

	@Transactional(noRollbackFor = ApiException.class)
	public TokenResponse login(LoginRequest request) {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.findByEmail(normalizeEmail(request.email()))
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_CREDENTIALS));

		if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
			throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_TEMPORARILY_LOCKED",
					"Tài khoản tạm khóa do đăng nhập sai nhiều lần, vui lòng thử lại sau");
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			recordFailedLogin(user, now);
			throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_CREDENTIALS);
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản đã bị khóa");
		}
		if (user.getEmailVerifiedAt() == null) {
			throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Vui lòng xác thực email trước khi đăng nhập");
		}

		user.setFailedLoginCount(0);
		user.setLockedUntil(null);
		return issueTokens(user, now);
	}

	@Transactional(noRollbackFor = ApiException.class)
	public TokenResponse refresh(String rawRefreshToken) {
		LocalDateTime now = LocalDateTime.now();
		RefreshToken current = refreshTokenRepository.findByTokenHash(TokenUtils.sha256Hex(rawRefreshToken))
				.orElseThrow(this::invalidRefreshToken);

		if (current.isRevoked()) {
			// Token đã được xoay vòng mà vẫn bị gửi lại: có khả năng bị đánh cắp, thu hồi toàn bộ phiên
			refreshTokenRepository.revokeAllActiveByUserId(current.getUser().getId(), now);
			throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED",
					"Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại");
		}
		if (current.isExpired(now)) {
			throw invalidRefreshToken();
		}
		User user = current.getUser();
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản đã bị khóa");
		}

		IssuedRefreshToken next = createRefreshToken(user, now);
		current.setRevokedAt(now);
		current.setReplacedBy(next.entity().getId());
		return new TokenResponse(jwtService.createAccessToken(user), next.rawToken(),
				"Bearer", jwtService.accessTokenTtlSeconds());
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenRepository.findByTokenHash(TokenUtils.sha256Hex(rawRefreshToken))
				.filter(token -> !token.isRevoked())
				.ifPresent(token -> token.setRevokedAt(LocalDateTime.now()));
	}

	@Transactional
	public void forgotPassword(String email) {
		userRepository.findByEmail(normalizeEmail(email))
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.ifPresent(user -> {
					String token = issueAccountToken(user, AccountTokenPurpose.PASSWORD_RESET,
							Duration.ofMinutes(properties.auth().resetTtlMinutes()));
					emailSender.send(user.getEmail(), "Đặt lại mật khẩu", buildLink("/reset-password", token));
				});
	}

	@Transactional
	public void resetPassword(String rawToken, String newPassword) {
		AccountToken token = findUsableToken(rawToken, AccountTokenPurpose.PASSWORD_RESET);
		LocalDateTime now = LocalDateTime.now();
		User user = token.getUser();

		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setFailedLoginCount(0);
		user.setLockedUntil(null);
		// Người dùng nhận được email đặt lại nghĩa là đã chứng minh quyền sở hữu email
		if (user.getEmailVerifiedAt() == null) {
			user.setEmailVerifiedAt(now);
		}
		token.setUsedAt(now);
		accountTokenRepository.invalidateActiveByUserIdAndPurpose(user.getId(), AccountTokenPurpose.PASSWORD_RESET, now);
		refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
	}

	private AccountToken findUsableToken(String rawToken, AccountTokenPurpose purpose) {
		LocalDateTime now = LocalDateTime.now();
		return accountTokenRepository.findByTokenHashAndPurpose(TokenUtils.sha256Hex(rawToken), purpose)
				.filter(token -> token.getUsedAt() == null && token.getExpiresAt().isAfter(now))
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN",
						"Token không hợp lệ hoặc đã hết hạn"));
	}

	/** Vô hiệu hóa các token cùng loại còn hiệu lực rồi cấp token mới. Trả về token gốc để gửi qua email. */
	private String issueAccountToken(User user, AccountTokenPurpose purpose, Duration ttl) {
		LocalDateTime now = LocalDateTime.now();
		accountTokenRepository.invalidateActiveByUserIdAndPurpose(user.getId(), purpose, now);
		String raw = TokenUtils.generateToken();
		accountTokenRepository.save(new AccountToken(user, purpose, TokenUtils.sha256Hex(raw), now.plus(ttl)));
		return raw;
	}

	private void recordFailedLogin(User user, LocalDateTime now) {
		int failures = user.getFailedLoginCount() + 1;
		if (failures >= properties.auth().maxFailedLogins()) {
			user.setFailedLoginCount(0);
			user.setLockedUntil(now.plusMinutes(properties.auth().lockMinutes()));
		} else {
			user.setFailedLoginCount(failures);
		}
	}

	private TokenResponse issueTokens(User user, LocalDateTime now) {
		IssuedRefreshToken refreshToken = createRefreshToken(user, now);
		return new TokenResponse(jwtService.createAccessToken(user), refreshToken.rawToken(),
				"Bearer", jwtService.accessTokenTtlSeconds());
	}

	private IssuedRefreshToken createRefreshToken(User user, LocalDateTime now) {
		String raw = TokenUtils.generateToken();
		RefreshToken entity = new RefreshToken(UUID.randomUUID().toString(), user,
				TokenUtils.sha256Hex(raw), now.plusDays(properties.jwt().refreshTtlDays()));
		refreshTokenRepository.save(entity);
		return new IssuedRefreshToken(entity, raw);
	}

	private ApiException invalidRefreshToken() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
				"Refresh token không hợp lệ hoặc đã hết hạn");
	}

	private void sendVerificationEmail(User user, String token) {
		String body = """
				Xin chào,

				Cảm ơn bạn đã đăng ký tài khoản trên hệ thống tuyển dụng.
				Vui lòng xác thực email bằng cách bấm vào link sau trong vòng %d giờ:

				%s

				Nếu link không mở được, bạn có thể dùng token sau cho API xác thực email:
				%s

				Nếu bạn không đăng ký tài khoản này, hãy bỏ qua email này.

				Trân trọng,
				Đội ngũ Recruitment
				""".formatted(properties.auth().verifyTtlHours(), buildLink("/verify-email", token), token);
		emailSender.send(user.getEmail(), "Xác thực địa chỉ email", body);
	}

	private String buildLink(String path, String token) {
		return properties.frontendUrl() + path + "?token=" + token;
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	/** Refresh token vừa cấp: entity để lưu vào CSDL và chuỗi gốc để trả cho client. */
	private record IssuedRefreshToken(RefreshToken entity, String rawToken) {
	}
}
