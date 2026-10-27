package com.example.recruitment.config;

import com.example.recruitment.repository.UserRepository;
import com.example.recruitment.security.JwtAuthenticationFilter;
import com.example.recruitment.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
			"/api/auth/register",
			"/api/auth/login",
			"/api/auth/refresh",
			"/api/auth/logout",
			"/api/auth/verify-email",
			"/api/auth/resend-verification",
			"/api/auth/forgot-password",
			"/api/auth/reset-password",
			"/v3/api-docs",
			"/v3/api-docs/**",
			"/swagger-ui/**",
			"/swagger-ui.html",
			"/actuator/health"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
												   JwtService jwtService,
												   UserRepository userRepository) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) ->
								writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
										"Chưa đăng nhập hoặc token không hợp lệ"))
						.accessDeniedHandler((request, response, accessDeniedException) ->
								writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
										"Không có quyền thực hiện thao tác này")))
				.addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
						UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	private static void writeError(HttpServletResponse response, HttpStatus status, String code, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
	}
}
