package com.eams.service.impl;

import com.eams.dto.response.DashboardResponse;
import com.eams.entity.AssetStatus;
import com.eams.entity.AssignmentStatus;
import com.eams.entity.DamageStatus;
import com.eams.entity.Employee;
import com.eams.entity.MaintenanceStatus;
import com.eams.entity.RequestStatus;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.MaintenanceRepository;
import com.eams.security.CurrentUserService;
import com.eams.service.DashboardService;
import com.eams.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * All numbers come from COUNT / SUM queries in the database.
 * (Before, every count loaded the whole table into memory and called .size().)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final AssetRequestRepository requestRepository;
    private final DamageReportRepository damageReportRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final InsightsService insightsService;
    private final CurrentUserService currentUser;

    @Override
    public DashboardResponse getAdminDashboard() {
        DashboardResponse r = new DashboardResponse();
        fillAssets(r);
        fillRequests(r);
        fillDamage(r);
        fillMaintenance(r);
        r.setTotalEmployees(employeeRepository.count());
        r.setActiveAssignments(assignmentRepository.countByStatus(AssignmentStatus.ACTIVE));
        r.setReturnedAssignments(assignmentRepository.countByStatus(AssignmentStatus.RETURNED));
        r.setTotalRepairCost(maintenanceRepository.sumRepairCost());
        r.setAssetsByCategory(insightsService.assetsByCategory());
        r.setRepairCostByCategory(insightsService.repairCostByCategory());
        return r;
    }

    @Override
    public DashboardResponse getManagerDashboard() {
        DashboardResponse r = new DashboardResponse();
        fillAssets(r);
        fillRequests(r);
        r.setTotalEmployees(employeeRepository.count());
        r.setActiveAssignments(assignmentRepository.countByStatus(AssignmentStatus.ACTIVE));
        r.setTotalRepairCost(maintenanceRepository.sumRepairCost());
        r.setAssetsByCategory(insightsService.assetsByCategory());
        r.setRepairCostByCategory(insightsService.repairCostByCategory());
        return r;
    }

    @Override
    public DashboardResponse getEmployeeDashboard(Long employeeIdFromPath) {
        Employee me = currentUser.requireSelf(employeeIdFromPath);
        Long id = me.getId();

        DashboardResponse r = new DashboardResponse();
        r.setPendingRequests(requestRepository.countByEmployeeIdAndStatus(id, RequestStatus.PENDING));
        r.setApprovedRequests(requestRepository.countByEmployeeIdAndStatus(id, RequestStatus.APPROVED));
        r.setRejectedRequests(requestRepository.countByEmployeeIdAndStatus(id, RequestStatus.REJECTED));
        r.setTotalDamageReports(damageReportRepository.countByEmployeeId(id));
        r.setActiveAssignments(
                assignmentRepository.countByEmployeeIdAndStatus(id, AssignmentStatus.ACTIVE)
                        + assignmentRepository.countByEmployeeIdAndStatus(id, AssignmentStatus.RETURN_REQUESTED));
        r.setReturnedAssignments(assignmentRepository.countByEmployeeIdAndStatus(id, AssignmentStatus.RETURNED));
        return r;
    }

    @Override
    public DashboardResponse getTechnicianDashboard() {
        DashboardResponse r = new DashboardResponse();
        fillDamage(r);
        fillMaintenance(r);
        r.setMaintenanceAssets(assetRepository.countByStatus(AssetStatus.MAINTENANCE));
        r.setDamagedAssets(assetRepository.countByStatus(AssetStatus.DAMAGED));
        r.setTotalRepairCost(maintenanceRepository.sumRepairCost());
        return r;
    }

    // ---------------------------------------------------------

    private void fillAssets(DashboardResponse r) {
        r.setTotalAssets(assetRepository.count());
        r.setAvailableAssets(assetRepository.countByStatus(AssetStatus.AVAILABLE));
        r.setAssignedAssets(assetRepository.countByStatus(AssetStatus.ASSIGNED));
        r.setDamagedAssets(assetRepository.countByStatus(AssetStatus.DAMAGED));
        r.setMaintenanceAssets(assetRepository.countByStatus(AssetStatus.MAINTENANCE));
        r.setRetiredAssets(assetRepository.countByStatus(AssetStatus.RETIRED));
        LocalDate today = LocalDate.now();
        r.setWarrantyExpiringSoon(assetRepository.countWarrantyExpiringBetween(today, today.plusDays(30)));
    }

    private void fillRequests(DashboardResponse r) {
        r.setPendingRequests(requestRepository.countByStatus(RequestStatus.PENDING));
        r.setApprovedRequests(requestRepository.countByStatus(RequestStatus.APPROVED));
        r.setRejectedRequests(requestRepository.countByStatus(RequestStatus.REJECTED));
        r.setUnassignedApprovedRequests(requestRepository.countByStatus(RequestStatus.APPROVED));
    }

    private void fillDamage(DashboardResponse r) {
        r.setTotalDamageReports(damageReportRepository.count());
        r.setReportedDamage(damageReportRepository.countByStatus(DamageStatus.REPORTED));
        r.setUnderReviewDamage(damageReportRepository.countByStatus(DamageStatus.UNDER_REVIEW));
        r.setRepairingDamage(damageReportRepository.countByStatus(DamageStatus.REPAIRING));
        r.setResolvedDamage(damageReportRepository.countByStatus(DamageStatus.RESOLVED));
    }

    private void fillMaintenance(DashboardResponse r) {
        r.setTotalMaintenance(maintenanceRepository.count());
        r.setScheduledMaintenance(maintenanceRepository.countByStatus(MaintenanceStatus.SCHEDULED));
        r.setInProgressMaintenance(maintenanceRepository.countByStatus(MaintenanceStatus.IN_PROGRESS));
        r.setCompletedMaintenance(maintenanceRepository.countByStatus(MaintenanceStatus.COMPLETED));
        r.setCancelledMaintenance(maintenanceRepository.countByStatus(MaintenanceStatus.CANCELLED));
    }
}
