package com.eams.dto.request;

import com.eams.entity.AssetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetRequestDto {

    @NotBlank(message = "Asset code is required")
    private String assetCode;

    @NotBlank(message = "Asset name is required")
    private String name;

    private String description;

    private String serialNumber;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private LocalDate purchaseDate;

    @Positive(message = "Purchase price must be positive")
    private BigDecimal purchasePrice;

    private AssetStatus status = AssetStatus.AVAILABLE;

    private String location;
}