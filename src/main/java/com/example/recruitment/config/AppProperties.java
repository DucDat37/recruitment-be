package com.example.recruitment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String frontendUrl, Jwt jwt, Auth auth) {

	public record Jwt(String secret, long accessTtlMinutes, long refreshTtlDays) {
	}

	public record Auth(int maxFailedLogins, long lockMinutes, long verifyTtlHours, long resetTtlMinutes) {
	}
}
