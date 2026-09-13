package com.eams.dto.request;

import com.eams.entity.MaintenanceStatus;

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
public class MaintenanceUpdateRequest {

    private MaintenanceStatus status;

    private LocalDate endDate;

    @PositiveOrZero(
            message = "Repair cost cannot be negative"
    )
    private BigDecimal repairCost;

    @Size(
            max = 100,
            message = "Technician must not exceed 100 characters"
    )
    private String technician;

    @Size(
            max = 1000,
            message = "Notes must not exceed 1000 characters"
    )
    private String notes;
}