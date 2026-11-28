package com.example.recruitment.service;

import com.example.recruitment.common.SlugUtils;
import com.example.recruitment.dto.category.CategoryRequest;
import com.example.recruitment.dto.category.CategoryResponse;
import com.example.recruitment.dto.category.CategoryTreeNode;
import com.example.recruitment.entity.JobCategory;
import com.example.recruitment.exception.ApiException;
import com.example.recruitment.repository.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Job category management (UC-07). Categories have at most two levels: a category may only have
 * a root category as its parent, and a category that has children cannot itself become a child.
 */
@Service
@RequiredArgsConstructor
public class JobCategoryService {

	/** Column length of job_categories.slug. */
	static final int SLUG_MAX_LENGTH = 150;
	/** Leaves room for a "-NNN" suffix when the base slug is already taken. */
	private static final int SLUG_BASE_MAX_LENGTH = SLUG_MAX_LENGTH - 10;

	private final JobCategoryRepository categoryRepository;

	/** Returns root categories sorted by name, each with its children sorted by name. */
	@Transactional(readOnly = true)
	public List<CategoryTreeNode> getTree() {
		List<JobCategory> all = categoryRepository.findAllByOrderByNameAsc();
		Map<Long, List<CategoryTreeNode>> childrenByParent = new LinkedHashMap<>();
		for (JobCategory category : all) {
			if (!category.isRoot()) {
				childrenByParent.computeIfAbsent(category.getParentId(), key -> new ArrayList<>())
						.add(new CategoryTreeNode(category.getId(), category.getName(), category.getSlug(), List.of()));
			}
		}
		List<CategoryTreeNode> roots = new ArrayList<>();
		for (JobCategory category : all) {
			if (category.isRoot()) {
				roots.add(new CategoryTreeNode(category.getId(), category.getName(), category.getSlug(),
						childrenByParent.getOrDefault(category.getId(), List.of())));
			}
		}
		return roots;
	}

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		String name = request.name().trim();
		validateParent(request.parentId(), null);
		JobCategory category = new JobCategory(name, uniqueSlug(name, null), request.parentId());
		return CategoryResponse.from(categoryRepository.save(category));
	}

	/** Replaces name and parent. The slug is regenerated only when the name changes. */
	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		JobCategory category = findCategory(id);
		String name = request.name().trim();
		Long parentId = request.parentId();

		validateParent(parentId, id);
		if (parentId != null && categoryRepository.existsByParentId(id)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_HAS_CHILDREN",
					"Danh mục đang có danh mục con nên không thể chuyển thành danh mục con");
		}

		if (!name.equals(category.getName())) {
			category.setName(name);
			category.setSlug(uniqueSlug(name, id));
		}
		category.setParentId(parentId);
		return CategoryResponse.from(category);
	}

	/** Refuses to delete a category that still has children or is used by job postings. */
	@Transactional
	public void delete(Long id) {
		JobCategory category = findCategory(id);
		if (categoryRepository.existsByParentId(id)) {
			throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_HAS_CHILDREN",
					"Danh mục đang có danh mục con, hãy xóa hoặc chuyển các danh mục con trước");
		}
		if (categoryRepository.countJobsByCategoryId(id) > 0) {
			throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_IN_USE",
					"Danh mục đang được dùng trong tin tuyển dụng nên không thể xóa");
		}
		categoryRepository.delete(category);
	}

	/**
	 * Checks that the requested parent exists and is a root category, so the tree never exceeds two levels.
	 *
	 * @param selfId id of the category being updated, or {@code null} when creating
	 */
	private void validateParent(Long parentId, Long selfId) {
		if (parentId == null) {
			return;
		}
		if (parentId.equals(selfId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARENT",
					"Danh mục không thể là danh mục cha của chính nó");
		}
		JobCategory parent = categoryRepository.findById(parentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PARENT_NOT_FOUND",
						"Không tìm thấy danh mục cha"));
		if (!parent.isRoot()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARENT",
					"Danh mục chỉ có tối đa 2 cấp, danh mục cha phải là danh mục gốc");
		}
	}

	/**
	 * Generates a slug from the name and appends "-2", "-3"... until it is unique.
	 *
	 * @param selfId id of the category being updated, so its own current slug does not count as taken
	 */
	private String uniqueSlug(String name, Long selfId) {
		String base = SlugUtils.toSlug(name, SLUG_BASE_MAX_LENGTH);
		if (base.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_NAME",
					"Tên danh mục phải chứa ít nhất một chữ cái hoặc chữ số");
		}
		String slug = base;
		for (int suffix = 2; isSlugTaken(slug, selfId); suffix++) {
			slug = base + "-" + suffix;
		}
		return slug;
	}

	private boolean isSlugTaken(String slug, Long selfId) {
		return selfId == null ? categoryRepository.existsBySlug(slug) : categoryRepository.existsBySlugAndIdNot(slug, selfId);
	}

	private JobCategory findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
						"Không tìm thấy danh mục"));
	}
}
