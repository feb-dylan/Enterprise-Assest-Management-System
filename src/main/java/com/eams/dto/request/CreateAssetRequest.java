package com.eams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateAssetRequest {

    @NotBlank(message = "Asset tag is required")
    private String assetTag;

    @NotBlank(message = "Asset name is required")
    private String name;

    private String serialNumber;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private Long departmentId;

    private LocalDate purchaseDate;

    private BigDecimal purchaseCost;

    private String specification;
}