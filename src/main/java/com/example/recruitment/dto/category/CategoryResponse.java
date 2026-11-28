package com.example.recruitment.dto.category;

import com.example.recruitment.entity.JobCategory;

public record CategoryResponse(
		Long id,
		Long parentId,
		String name,
		String slug
) {

	public static CategoryResponse from(JobCategory category) {
		return new CategoryResponse(category.getId(), category.getParentId(), category.getName(), category.getSlug());
	}
}
