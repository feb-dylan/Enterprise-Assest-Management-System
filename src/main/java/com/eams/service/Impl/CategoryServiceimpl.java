package com.eams.service.impl;

import com.eams.dto.request.CategoryRequestDto;
import com.eams.dto.response.AssetResponseDto;
import com.eams.dto.response.CategoryResponseDto;
import com.eams.entity.Category;
import com.eams.exception.ConflictException;
import com.eams.exception.NotFoundException;
import com.eams.repository.AssetRepository;
import com.eams.repository.CategoryRepository;
import com.eams.service.CategoryService;
import com.eams.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final DtoMapper mapper;

    @Override
    public CategoryResponseDto createCategory(CategoryRequestDto dto) {
        String name = dto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category already exists: " + name);
        }
        Category category = new Category();
        apply(category, dto, name);
        return mapper.category(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream().map(mapper::category).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        return mapper.category(find(id));
    }

    @Override
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto dto) {
        Category category = find(id);
        String name = dto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Category name already exists: " + name);
        }
        apply(category, dto, name);
        return mapper.category(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = find(id);
        if (assetRepository.existsByCategoryId(id)) {
            throw new ConflictException(
                    "Cannot delete this category because it has assets assigned to it.");
        }
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAssetsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category not found with id: " + categoryId);
        }
        return assetRepository.findByCategoryId(categoryId).stream().map(mapper::asset).toList();
    }

    private void apply(Category category, CategoryRequestDto dto, String name) {
        category.setName(name);
        category.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        category.setExpectedLifespanMonths(dto.getExpectedLifespanMonths());
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
    }
}
