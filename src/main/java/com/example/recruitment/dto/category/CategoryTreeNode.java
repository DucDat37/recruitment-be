package com.example.recruitment.dto.category;

import java.util.List;

/** One node of the public category tree; leaf nodes have an empty {@code children} list. */
public record CategoryTreeNode(
		Long id,
		String name,
		String slug,
		List<CategoryTreeNode> children
) {
}
