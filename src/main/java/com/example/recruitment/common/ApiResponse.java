package com.example.recruitment.common;

import java.time.Instant;

/**
 * Unified envelope for every successful REST response.
 *
 * <p>Errors are not wrapped in this type; they are returned as
 * {@link com.example.recruitment.exception.ErrorResponse} by the global exception handler.</p>
 *
 * @param success   always {@code true} for responses built with this type
 * @param message   human-readable message shown to the user (Vietnamese)
 * @param data      response payload, or {@code null} when the operation returns no data
 * @param timestamp server time when the response was created
 * @param <T>       payload type
 */
public record ApiResponse<T>(
		boolean success,
		String message,
		T data,
		Instant timestamp
) {

	/** Builds a successful response carrying a payload. */
	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(true, message, data, Instant.now());
	}

	/** Builds a successful response for operations that return no payload. */
	public static ApiResponse<Void> success(String message) {
		return new ApiResponse<>(true, message, null, Instant.now());
	}
}
