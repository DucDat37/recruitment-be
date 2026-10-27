package com.example.recruitment.security;

import com.example.recruitment.config.AppProperties;
import com.example.recruitment.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtService {

	private final SecretKey key;
	private final Duration accessTtl;

	public JwtService(AppProperties properties) {
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.jwt().secret()));
		this.accessTtl = Duration.ofMinutes(properties.jwt().accessTtlMinutes());
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(user.getId()))
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(accessTtl)))
				.signWith(key)
				.compact();
	}

	/** Trả về id người dùng nếu token hợp lệ và chưa hết hạn. */
	public Optional<Long> parseUserId(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return Optional.of(Long.parseLong(claims.getSubject()));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public long accessTokenTtlSeconds() {
		return accessTtl.toSeconds();
	}
}
