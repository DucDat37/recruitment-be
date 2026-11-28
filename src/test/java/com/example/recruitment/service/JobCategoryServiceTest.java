package com.example.recruitment.service;

import com.example.recruitment.dto.category.CategoryRequest;
import com.example.recruitment.dto.category.CategoryResponse;
import com.example.recruitment.dto.category.CategoryTreeNode;
import com.example.recruitment.entity.JobCategory;
import com.example.recruitment.exception.ApiException;
import com.example.recruitment.repository.JobCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobCategoryServiceTest {

	@Mock
	private JobCategoryRepository categoryRepository;

	@InjectMocks
	private JobCategoryService categoryService;

	private static JobCategory category(Long id, String name, String slug, Long parentId) {
		JobCategory category = new JobCategory(name, slug, parentId);
		category.setId(id);
		return category;
	}

	private static String errorCode(Throwable ex) {
		return ((ApiException) ex).getCode();
	}

	@Test
	void getTreeGroupsChildrenUnderTheirRoot() {
		when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
				category(1L, "Công nghệ thông tin", "cong-nghe-thong-tin", null),
				category(3L, "Backend", "backend", 1L),
				category(2L, "Kế toán", "ke-toan", null),
				category(4L, "Frontend", "frontend", 1L)));

		List<CategoryTreeNode> tree = categoryService.getTree();

		assertThat(tree).extracting(CategoryTreeNode::id).containsExactly(1L, 2L);
		assertThat(tree.get(0).children()).extracting(CategoryTreeNode::id).containsExactly(3L, 4L);
		assertThat(tree.get(1).children()).isEmpty();
	}

	@Test
	void createGeneratesSlugFromName() {
		when(categoryRepository.existsBySlug("cong-nghe-thong-tin")).thenReturn(false);
		when(categoryRepository.save(any(JobCategory.class))).thenAnswer(invocation -> {
			JobCategory saved = invocation.getArgument(0);
			saved.setId(10L);
			return saved;
		});

		CategoryResponse response = categoryService.create(new CategoryRequest("  Công nghệ thông tin ", null));

		assertThat(response).isEqualTo(new CategoryResponse(10L, null, "Công nghệ thông tin", "cong-nghe-thong-tin"));
	}

	@Test
	void createAppendsSuffixWhenSlugIsTaken() {
		when(categoryRepository.existsBySlug("backend")).thenReturn(true);
		when(categoryRepository.existsBySlug("backend-2")).thenReturn(false);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "IT", "it", null)));
		when(categoryRepository.save(any(JobCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CategoryResponse response = categoryService.create(new CategoryRequest("Backend", 1L));

		assertThat(response.slug()).isEqualTo("backend-2");
		assertThat(response.parentId()).isEqualTo(1L);
	}

	@Test
	void createRejectsNameWithoutLettersOrDigits() {
		assertThatThrownBy(() -> categoryService.create(new CategoryRequest("!!!", null)))
				.satisfies(ex -> assertThat(errorCode(ex)).isEqualTo("INVALID_CATEGORY_NAME"));
	}

	@Test
	void createRejectsMissingParent() {
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Backend", 99L)))
				.satisfies(ex -> assertThat(errorCode(ex)).isEqualTo("PARENT_NOT_FOUND"));
	}

	@Test
	void createRejectsThirdLevel() {
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, "Backend", "backend", 1L)));

		assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Java", 3L)))
				.satisfies(ex -> assertThat(errorCode(ex)).isEqualTo("INVALID_PARENT"));
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void updateRejectsSelfAsParent() {
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "IT", "it", null)));

		assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequest("IT", 1L)))
				.satisfies(ex -> assertThat(errorCode(ex)).isEqualTo("INVALID_PARENT"));
	}

	@Test
	void updateRejectsMovingCategoryWithChildrenUnderAnotherRoot() {
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "IT", "it", null)));
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, "Kế toán", "ke-toan", null)));
		when(categoryRepository.existsByParentId(1L)).thenReturn(true);

		assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequest("IT", 2L)))
				.satisfies(ex -> assertThat(errorCode(ex)).isEqualTo("CATEGORY_HAS_CHILDREN"));
	}

	@Test
	void updateKeepsSlugWhenNameIsUnchanged() {
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, "Backend", "backend", 1L)));
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, "Kế toán", "ke-toan", null)));

		CategoryResponse response = categoryService.update(3L, new CategoryRequest("Backend", 2L));

		assertThat(response).isEqualTo(new CategoryResponse(3L, 2L, "Backend", "backend"));
	}

	@Test
	void updateRegeneratesSlugIgnoringItsOwnCurrentSlug() {
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, "Back end", "back-end", null)));
		when(categoryRepository.existsBySlugAndIdNot("backend", 3L)).thenReturn(false);

		CategoryResponse response = categoryService.update(3L, new CategoryRequest("Backend", null));

		assertThat(response.slug()).isEqualTo("backend");
	}

	@Test
	void updateRejectsUnknownCategory() {
		when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.update(5L, new CategoryRequest("X", null)))
				.satisfies(ex -> assertThat(errorCode(ex)).isEqualTo("CATEGORY_NOT_FOUND"));
	}

	@Test
	void deleteRejectsCategoryWithChildren() {
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "IT", "it", null)));
		when(categoryRepository.existsByParentId(1L)).thenReturn(true);

		assertThatThrownBy(() -> categoryService.delete(1L))
				.satisfies(ex -> assertThat(errorCode(ex)).isEqualTo("CATEGORY_HAS_CHILDREN"));
		verify(categoryRepository, never()).delete(any());
	}

	@Test
	void deleteRejectsCategoryUsedByJobs() {
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(category(3L, "Backend", "backend", 1L)));
		when(categoryRepository.existsByParentId(3L)).thenReturn(false);
		when(categoryRepository.countJobsByCategoryId(3L)).thenReturn(2L);

		assertThatThrownBy(() -> categoryService.delete(3L))
				.satisfies(ex -> assertThat(errorCode(ex)).isEqualTo("CATEGORY_IN_USE"));
		verify(categoryRepository, never()).delete(any());
	}

	@Test
	void deleteRemovesUnusedLeafCategory() {
		JobCategory backend = category(3L, "Backend", "backend", 1L);
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(backend));
		when(categoryRepository.existsByParentId(3L)).thenReturn(false);
		when(categoryRepository.countJobsByCategoryId(3L)).thenReturn(0L);

		categoryService.delete(3L);

		verify(categoryRepository).delete(backend);
	}
}
