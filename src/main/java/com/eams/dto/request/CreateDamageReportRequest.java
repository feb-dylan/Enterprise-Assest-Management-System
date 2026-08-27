package com.eams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateDamageReportRequest {

    @NotNull(message = "Asset ID is required")
    private Long assetId;

    @NotNull(message = "Reporting employee ID is required")
    private Long reportedByEmployeeId;

    @NotBlank(message = "Damage description is required")
    private String description;

    @NotNull(message = "Incident date is required")
    private LocalDate incidentDate;

    private String imageUrl;
}