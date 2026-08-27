package com.eams.dto.request;

import com.eams.entity.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateMaintenanceStatusRequest {

    @NotNull(message = "Maintenance status is required")
    private MaintenanceStatus status;

    private LocalDate completionDate;

    private BigDecimal cost;

    private String remarks;
}