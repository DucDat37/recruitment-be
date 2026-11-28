package com.example.recruitment.controller;

import com.example.recruitment.common.ApiResponse;
import com.example.recruitment.dto.category.CategoryRequest;
import com.example.recruitment.dto.category.CategoryResponse;
import com.example.recruitment.service.JobCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Category management for administrators (UC-07). Access is restricted to ADMIN by SecurityConfig. */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

	private final JobCategoryService categoryService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
		return ApiResponse.success(categoryService.create(request), "Tạo danh mục thành công.");
	}

	@PutMapping("/{id}")
	public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return ApiResponse.success(categoryService.update(id, request), "Cập nhật danh mục thành công.");
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		categoryService.delete(id);
		return ApiResponse.success("Xóa danh mục thành công.");
	}
}
