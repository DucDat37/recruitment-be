package com.example.recruitment.security;

import com.example.recruitment.entity.enums.UserStatus;
import com.example.recruitment.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Đọc access token từ header Authorization. Tài khoản không ở trạng thái ACTIVE
 * sẽ không được xác thực, nên token cũ cũng bị từ chối ngay khi tài khoản bị khóa.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			jwtService.parseUserId(header.substring(BEARER_PREFIX.length()))
					.flatMap(userRepository::findById)
					.filter(user -> user.getStatus() == UserStatus.ACTIVE)
					.ifPresent(user -> {
						AuthUser principal = new AuthUser(user.getId(), user.getRole());
						SecurityContextHolder.getContext().setAuthentication(
								new UsernamePasswordAuthenticationToken(principal, null,
										List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
					});
		}
		filterChain.doFilter(request, response);
	}
}
