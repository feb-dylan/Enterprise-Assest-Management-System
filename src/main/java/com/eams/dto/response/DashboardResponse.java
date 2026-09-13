package com.eams.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    // =========================================================
    // ASSETS
    // =========================================================

    private long totalAssets;

    private long availableAssets;

    private long assignedAssets;

    private long damagedAssets;

    private long maintenanceAssets;

    private long retiredAssets;


    // =========================================================
    // EMPLOYEES
    // =========================================================

    private long totalEmployees;


    // =========================================================
    // REQUESTS
    // =========================================================

    private long pendingRequests;

    private long approvedRequests;

    private long rejectedRequests;


    // =========================================================
    // DAMAGE
    // =========================================================

    private long totalDamageReports;

    private long reportedDamage;

    private long underReviewDamage;

    private long repairingDamage;

    private long resolvedDamage;


    // =========================================================
    // MAINTENANCE
    // =========================================================

    private long totalMaintenance;

    private long scheduledMaintenance;

    private long inProgressMaintenance;

    private long completedMaintenance;

    private long cancelledMaintenance;


    // =========================================================
    // ASSIGNMENTS
    // =========================================================

    private long activeAssignments;

    private long returnedAssignments;


    // =========================================================
    // COST
    // =========================================================

    private double totalRepairCost;
}