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

    /**
     * Technician user to assign. If empty and the caller is a technician,
     * the job is assigned to the caller.
     */
    private Long technicianUserId;

    /** Legacy free-text technician name (still accepted). */
    @Size(max = 100, message = "Technician must not exceed 100 characters")
    private String technician;

    /** Optional: which REPAIRING damage report this job fixes (defaults to the latest one). */
    private Long damageReportId;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @PositiveOrZero(message = "Repair cost cannot be negative")
    private BigDecimal repairCost;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
