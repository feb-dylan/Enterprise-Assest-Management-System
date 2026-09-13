package com.eams.dto.response;

import com.eams.entity.AssetStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetResponseDto {

    private Long id;

    private String assetCode;

    private String name;

    private String description;

    private String serialNumber;

    private CategoryResponseDto category;

    private LocalDate purchaseDate;

    private BigDecimal purchasePrice;

    private AssetStatus status;

    private String location;

    private String imageUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}