package com.example.recruitment.controller;

import com.example.recruitment.common.ApiResponse;
import com.example.recruitment.dto.category.CategoryTreeNode;
import com.example.recruitment.service.JobCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public read access to the job category tree (UC-07). */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final JobCategoryService categoryService;

	@GetMapping
	public ApiResponse<List<CategoryTreeNode>> getTree() {
		return ApiResponse.success(categoryService.getTree(), "Lấy danh mục nghề thành công.");
	}
}
