package com.example.recruitment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String frontendUrl, Cors cors, Jwt jwt, Auth auth) {

	/** Origins allowed to call the API from a browser (exact scheme, host and port, no trailing slash). */
	public record Cors(List<String> allowedOrigins) {
	}

	public record Jwt(String secret, long accessTtlMinutes, long refreshTtlDays) {
	}

	public record Auth(int maxFailedLogins, long lockMinutes, long verifyTtlHours, long resetTtlMinutes) {
	}
}
