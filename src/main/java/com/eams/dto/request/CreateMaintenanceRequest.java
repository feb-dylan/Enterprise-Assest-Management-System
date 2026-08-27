package com.eams.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateMaintenanceRequest {

    @NotNull(message = "Asset ID is required")
    private Long assetId;

    private Long damageReportId;

    private String technicianName;

    private String maintenanceType;

    private BigDecimal cost;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate completionDate;

    private String remarks;
}