package com.eams.dto.response;

import com.eams.entity.MaintenanceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MaintenanceResponse {
    private Long id;
    private String workOrderNumber;
    private Long assetId;
    private String assetTag;
    private String assetName;
    private Long damageReportId;
    private String damageReportNumber;
    private String technicianName;
    private String maintenanceType;
    private BigDecimal cost;
    private LocalDate startDate;
    private LocalDate completionDate;
    private MaintenanceStatus status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}