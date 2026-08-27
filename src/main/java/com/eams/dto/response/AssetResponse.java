package com.eams.dto.response;

import com.eams.entity.AssetStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AssetResponse {
    private Long id;
    private String assetTag;
    private String name;
    private String serialNumber;
    private Long categoryId;
    private String categoryName;
    private Long departmentId;
    private String departmentName;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private AssetStatus status;
    private String qrCodeUrl;
    private String imageUrl;
    private String specification;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}