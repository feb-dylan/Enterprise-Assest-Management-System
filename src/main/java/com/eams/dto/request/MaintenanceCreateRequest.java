package com.eams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class MaintenanceCreateRequest {

    @NotNull(message = "Asset ID is required")
    private Long assetId;

    @Size(
            max = 100,
            message = "Technician must not exceed 100 characters"
    )
    private String technician;

    @NotBlank(message = "Description is required")
    @Size(
            max = 2000,
            message = "Description must not exceed 2000 characters"
    )
    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @PositiveOrZero(
            message = "Repair cost cannot be negative"
    )
    private BigDecimal repairCost;

    private String notes;
}