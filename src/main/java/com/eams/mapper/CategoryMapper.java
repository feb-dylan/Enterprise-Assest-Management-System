package com.eams.mapper;

import com.eams.dto.request.CreateCategoryRequest;
import com.eams.dto.response.CategoryResponse;
import com.eams.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CreateCategoryRequest request) {
        if (request == null) return null;
        Category category = new Category();
        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setDescription(request.getDescription());
        return category;
    }

    public CategoryResponse toResponse(Category entity) {
        if (entity == null) return null;
        CategoryResponse response = new CategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setCode(entity.getCode());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}