package com.example.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Job category, organised in at most two levels: root categories (no parent) and their children.
 *
 * <p>The parent is kept as a plain id instead of a {@code @ManyToOne} because the whole tree is
 * small and is always assembled in memory from a single query.</p>
 */
@Entity
@Table(name = "job_categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "parent_id")
	private Long parentId;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, unique = true, length = 150)
	private String slug;

	public JobCategory(String name, String slug, Long parentId) {
		this.name = name;
		this.slug = slug;
		this.parentId = parentId;
	}

	public boolean isRoot() {
		return parentId == null;
	}
}
