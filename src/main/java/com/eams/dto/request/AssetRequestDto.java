package com.eams.dto.request;

import com.eams.entity.AssetStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetRequestDto {

    @NotBlank
    @Size(max = 50)
    private String assetCode;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 100)
    private String serialNumber;

    @NotNull
    private Long categoryId;

    private LocalDate purchaseDate;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal purchasePrice;

    private AssetStatus status;

    @Size(max = 255)
    private String location;
}