package com.example.recruitment.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a category. The slug is generated from the name.
 *
 * @param parentId {@code null} for a root category, otherwise the id of a root category
 */
public record CategoryRequest(
		@NotBlank @Size(max = 120) String name,
		Long parentId
) {
}
