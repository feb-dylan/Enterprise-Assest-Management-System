package com.eams.service;

import com.eams.dto.request.CategoryRequestDto;
import com.eams.dto.response.CategoryResponseDto;

import java.util.List;

public interface CategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto requestDto);
    List<CategoryResponseDto> getAllCategories();
    CategoryResponseDto getCategoryById(Long id);
    CategoryResponseDto updateCategory(Long id, CategoryRequestDto requestDto);
    void deleteCategory(Long id);
}