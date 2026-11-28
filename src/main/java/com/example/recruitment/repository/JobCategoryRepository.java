package com.example.recruitment.repository;

import com.example.recruitment.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {

	List<JobCategory> findAllByOrderByNameAsc();

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);

	boolean existsByParentId(Long parentId);

	/** Native query because the Job entity does not exist yet (UC-23). */
	@Query(value = "SELECT COUNT(*) FROM jobs WHERE category_id = :categoryId", nativeQuery = true)
	long countJobsByCategoryId(@Param("categoryId") Long categoryId);
}
