package com.eams.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardSummaryResponse {

    // Asset Metrics
    private long totalAssets;
    private long availableAssets;
    private long assignedAssets;
    private long underRepairAssets;
    private long damagedAssets;

    // Organization Metrics
    private long totalEmployees;
    private long totalDepartments;
    private long totalCategories;

    // Operational Metrics
    private long pendingDamageReports;
    private long activeMaintenances;

    // Financial Metrics
    private BigDecimal totalMaintenanceCost;
}