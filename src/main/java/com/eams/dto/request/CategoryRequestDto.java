package com.eams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequestDto {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    /** Optional: expected useful life in months (default used when empty: 48). */
    @jakarta.validation.constraints.Min(value = 1, message = "Expected lifespan must be at least 1 month")
    @jakarta.validation.constraints.Max(value = 600, message = "Expected lifespan must not exceed 600 months")
    private Integer expectedLifespanMonths;
}
