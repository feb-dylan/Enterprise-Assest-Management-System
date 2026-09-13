package com.eams.dto.response;

import com.eams.entity.MaintenanceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceResponse {

    private Long id;

    private Long assetId;

    private String assetCode;

    private String assetName;

    private String technician;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal repairCost;

    private MaintenanceStatus status;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}