package com.eams.dto.response;

import com.eams.entity.DamageStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DamageReportResponse {
    private Long id;
    private String reportNumber;
    private Long assetId;
    private String assetTag;
    private String assetName;
    private Long reportedByEmployeeId;
    private String reportedByEmployeeName;
    private String description;
    private LocalDate incidentDate;
    private DamageStatus status;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}