package com.eams.service.impl;

import com.eams.dto.request.CategoryRequestDto;
import com.eams.dto.response.AssetResponseDto;
import com.eams.dto.response.CategoryResponseDto;
import com.eams.entity.Asset;
import com.eams.entity.Category;
import com.eams.repository.AssetRepository;
import com.eams.repository.CategoryRepository;
import com.eams.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            AssetRepository assetRepository) {

        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
    }

    @Override
    public CategoryResponseDto createCategory(
            CategoryRequestDto requestDto) {

        String name = requestDto.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException(
                    "Category already exists: " + name
            );
        }

        Category category = new Category();

        category.setName(name);
        category.setDescription(
                requestDto.getDescription() != null
                        ? requestDto.getDescription().trim()
                        : null
        );

        Category savedCategory =
                categoryRepository.save(category);

        return mapToResponseDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found with id: " + id
                        )
                );

        return mapToResponseDto(category);
    }

    @Override
    public CategoryResponseDto updateCategory(
            Long id,
            CategoryRequestDto requestDto) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found with id: " + id
                        )
                );

        String name = requestDto.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(
                name,
                id
        )) {
            throw new RuntimeException(
                    "Category name already exists: " + name
            );
        }

        category.setName(name);

        category.setDescription(
                requestDto.getDescription() != null
                        ? requestDto.getDescription().trim()
                        : null
        );

        Category updatedCategory =
                categoryRepository.save(category);

        return mapToResponseDto(updatedCategory);
    }

    @Override
    public void deleteCategory(Long id) {

        // 1. Check whether category exists
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found with id: " + id
                        )
                );

        // 2. Check whether assets are assigned
        if (assetRepository.existsByCategoryId(id)) {

            throw new IllegalArgumentException(
                    "Cannot delete this category because it has assets assigned to it."
            );
        }

        // 3. Delete category
        categoryRepository.delete(category);
    }

    // =========================================================
    // GET ASSETS BY CATEGORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAssetsByCategory(
            Long categoryId) {

        // Check whether category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException(
                    "Category not found with id: " + categoryId
            );
        }

        // Get all assets belonging to this category
        return assetRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::mapAssetToResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // CATEGORY MAPPER
    // =========================================================

    private CategoryResponseDto mapToResponseDto(
            Category category) {

        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    // =========================================================
    // ASSET MAPPER
    // =========================================================

    private AssetResponseDto mapAssetToResponseDto(
            Asset asset) {

        CategoryResponseDto categoryDto = null;

        if (asset.getCategory() != null) {
            categoryDto = CategoryResponseDto.builder()
                    .id(asset.getCategory().getId())
                    .name(asset.getCategory().getName())
                    .description(
                            asset.getCategory().getDescription()
                    )
                    .build();
        }

        return AssetResponseDto.builder()
                .id(asset.getId())
                .assetCode(asset.getAssetCode())
                .name(asset.getName())
                .description(asset.getDescription())
                .serialNumber(asset.getSerialNumber())
                .category(categoryDto)
                .purchaseDate(asset.getPurchaseDate())
                .purchasePrice(asset.getPurchasePrice())
                .status(asset.getStatus())
                .location(asset.getLocation())
                .imageUrl(asset.getImageUrl())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}