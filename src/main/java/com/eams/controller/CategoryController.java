package com.eams.controller;

import com.eams.dto.request.CategoryRequestDto;
import com.eams.dto.response.AssetResponseDto;
import com.eams.dto.response.CategoryResponseDto;
import com.eams.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(
            @Valid @RequestBody CategoryRequestDto requestDto) {

        CategoryResponseDto createdCategory =
                categoryService.createCategory(requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCategory);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(
                categoryService.getAllCategories()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getCategoryById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                categoryService.getCategoryById(id)
        );
    }

    // =========================================================
    // GET ASSETS UNDER CATEGORY
    // =========================================================

    @GetMapping("/{id}/assets")
    public ResponseEntity<List<AssetResponseDto>> getAssetsByCategory(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                categoryService.getAssetsByCategory(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto requestDto) {

        return ResponseEntity.ok(
                categoryService.updateCategory(id, requestDto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(
            @PathVariable Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.ok(
                "Category deleted successfully"
        );
    }
}