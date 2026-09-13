package com.eams.service.impl;

import com.eams.dto.response.DashboardResponse;

import com.eams.entity.*;

import com.eams.repository.*;

import com.eams.service.DashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl
        implements DashboardService {

    private final AssetRepository assetRepository;

    private final EmployeeRepository employeeRepository;

    private final AssetRequestRepository assetRequestRepository;

    private final DamageReportRepository damageReportRepository;

    private final MaintenanceRepository maintenanceRepository;

    private final AssetAssignmentRepository
            assetAssignmentRepository;


    // =========================================================
    // ADMIN DASHBOARD
    // =========================================================

    @Override
    public DashboardResponse getAdminDashboard() {

        DashboardResponse response =
                buildFullDashboard();


        return response;
    }


    // =========================================================
    // MANAGER DASHBOARD
    // =========================================================

    @Override
    public DashboardResponse getManagerDashboard() {

        DashboardResponse response =
                new DashboardResponse();


        response.setTotalEmployees(
                employeeRepository.count()
        );


        response.setTotalAssets(
                assetRepository.count()
        );


        response.setAvailableAssets(
                assetRepository
                        .findByStatus(
                                AssetStatus.AVAILABLE
                        )
                        .size()
        );


        response.setAssignedAssets(
                assetRepository
                        .findByStatus(
                                AssetStatus.ASSIGNED
                        )
                        .size()
        );


        response.setDamagedAssets(
                assetRepository
                        .findByStatus(
                                AssetStatus.DAMAGED
                        )
                        .size()
        );


        response.setMaintenanceAssets(
                assetRepository
                        .findByStatus(
                                AssetStatus.MAINTENANCE
                        )
                        .size()
        );


        response.setPendingRequests(
                assetRequestRepository
                        .findByStatus(
                                RequestStatus.PENDING
                        )
                        .size()
        );


        response.setApprovedRequests(
                assetRequestRepository
                        .findByStatus(
                                RequestStatus.APPROVED
                        )
                        .size()
        );


        response.setRejectedRequests(
                assetRequestRepository
                        .findByStatus(
                                RequestStatus.REJECTED
                        )
                        .size()
        );


        return response;
    }


    // =========================================================
    // EMPLOYEE DASHBOARD
    // =========================================================

    @Override
    public DashboardResponse getEmployeeDashboard(
            Long employeeId
    ) {

        DashboardResponse response =
                new DashboardResponse();


        response.setPendingRequests(
                assetRequestRepository
                        .findByEmployeeIdAndStatus(
                                employeeId,
                                RequestStatus.PENDING
                        )
                        .size()
        );


        response.setApprovedRequests(
                assetRequestRepository
                        .findByEmployeeIdAndStatus(
                                employeeId,
                                RequestStatus.APPROVED
                        )
                        .size()
        );


        response.setRejectedRequests(
                assetRequestRepository
                        .findByEmployeeIdAndStatus(
                                employeeId,
                                RequestStatus.REJECTED
                        )
                        .size()
        );


        response.setTotalDamageReports(
                damageReportRepository
                        .findByEmployeeId(
                                employeeId
                        )
                        .size()
        );


        response.setActiveAssignments(
                assetAssignmentRepository
                        .findByEmployeeId(
                                employeeId
                        )
                        .stream()
                        .filter(a ->
                                a.getStatus()
                                        == AssignmentStatus.ACTIVE
                        )
                        .count()
        );


        response.setReturnedAssignments(
                assetAssignmentRepository
                        .findByEmployeeId(
                                employeeId
                        )
                        .stream()
                        .filter(a ->
                                a.getStatus()
                                        == AssignmentStatus.RETURNED
                        )
                        .count()
        );


        return response;
    }


    // =========================================================
    // TECHNICIAN DASHBOARD
    // =========================================================

    @Override
    public DashboardResponse getTechnicianDashboard() {

        DashboardResponse response =
                new DashboardResponse();


        response.setTotalDamageReports(
                damageReportRepository.count()
        );


        response.setReportedDamage(
                damageReportRepository
                        .findByStatus(
                                DamageStatus.REPORTED
                        )
                        .size()
        );


        response.setUnderReviewDamage(
                damageReportRepository
                        .findByStatus(
                                DamageStatus.UNDER_REVIEW
                        )
                        .size()
        );


        response.setRepairingDamage(
                damageReportRepository
                        .findByStatus(
                                DamageStatus.REPAIRING
                        )
                        .size()
        );


        response.setResolvedDamage(
                damageReportRepository
                        .findByStatus(
                                DamageStatus.RESOLVED
                        )
                        .size()
        );


        response.setTotalMaintenance(
                maintenanceRepository.count()
        );


        response.setScheduledMaintenance(
                maintenanceRepository
                        .findByStatus(
                                MaintenanceStatus.SCHEDULED
                        )
                        .size()
        );


        response.setInProgressMaintenance(
                maintenanceRepository
                        .findByStatus(
                                MaintenanceStatus.IN_PROGRESS
                        )
                        .size()
        );


        response.setCompletedMaintenance(
                maintenanceRepository
                        .findByStatus(
                                MaintenanceStatus.COMPLETED
                        )
                        .size()
        );


        response.setCancelledMaintenance(
                maintenanceRepository
                        .findByStatus(
                                MaintenanceStatus.CANCELLED
                        )
                        .size()
        );


        response.setMaintenanceAssets(
                assetRepository
                        .findByStatus(
                                AssetStatus.MAINTENANCE
                        )
                        .size()
        );


        response.setDamagedAssets(
                assetRepository
                        .findByStatus(
                                AssetStatus.DAMAGED
                        )
                        .size()
        );


        response.setTotalRepairCost(
                calculateRepairCost()
        );


        return response;
    }


    // =========================================================
    // FULL ADMIN DASHBOARD
    // =========================================================

    private DashboardResponse buildFullDashboard() {

        DashboardResponse response =
                new DashboardResponse();


        // Assets

        response.setTotalAssets(
                assetRepository.count()
        );

        response.setAvailableAssets(
                countAssets(AssetStatus.AVAILABLE)
        );

        response.setAssignedAssets(
                countAssets(AssetStatus.ASSIGNED)
        );

        response.setDamagedAssets(
                countAssets(AssetStatus.DAMAGED)
        );

        response.setMaintenanceAssets(
                countAssets(AssetStatus.MAINTENANCE)
        );

        response.setRetiredAssets(
                countAssets(AssetStatus.RETIRED)
        );


        // Employees

        response.setTotalEmployees(
                employeeRepository.count()
        );


        // Requests

        response.setPendingRequests(
                assetRequestRepository
                        .findByStatus(
                                RequestStatus.PENDING
                        )
                        .size()
        );

        response.setApprovedRequests(
                assetRequestRepository
                        .findByStatus(
                                RequestStatus.APPROVED
                        )
                        .size()
        );

        response.setRejectedRequests(
                assetRequestRepository
                        .findByStatus(
                                RequestStatus.REJECTED
                        )
                        .size()
        );


        // Damage

        response.setTotalDamageReports(
                damageReportRepository.count()
        );

        response.setReportedDamage(
                countDamage(DamageStatus.REPORTED)
        );

        response.setUnderReviewDamage(
                countDamage(DamageStatus.UNDER_REVIEW)
        );

        response.setRepairingDamage(
                countDamage(DamageStatus.REPAIRING)
        );

        response.setResolvedDamage(
                countDamage(DamageStatus.RESOLVED)
        );


        // Maintenance

        response.setTotalMaintenance(
                maintenanceRepository.count()
        );

        response.setScheduledMaintenance(
                countMaintenance(
                        MaintenanceStatus.SCHEDULED
                )
        );

        response.setInProgressMaintenance(
                countMaintenance(
                        MaintenanceStatus.IN_PROGRESS
                )
        );

        response.setCompletedMaintenance(
                countMaintenance(
                        MaintenanceStatus.COMPLETED
                )
        );

        response.setCancelledMaintenance(
                countMaintenance(
                        MaintenanceStatus.CANCELLED
                )
        );


        // Assignments

        response.setActiveAssignments(
                assetAssignmentRepository
                        .findByStatus(
                                AssignmentStatus.ACTIVE
                        )
                        .size()
        );

        response.setReturnedAssignments(
                assetAssignmentRepository
                        .findByStatus(
                                AssignmentStatus.RETURNED
                        )
                        .size()
        );


        // Repair costs

        response.setTotalRepairCost(
                calculateRepairCost()
        );


        return response;
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private long countAssets(
            AssetStatus status
    ) {

        return assetRepository
                .findByStatus(status)
                .size();
    }


    private long countDamage(
            DamageStatus status
    ) {

        return damageReportRepository
                .findByStatus(status)
                .size();
    }


    private long countMaintenance(
            MaintenanceStatus status
    ) {

        return maintenanceRepository
                .findByStatus(status)
                .size();
    }


    private double calculateRepairCost() {

        return maintenanceRepository
                .findAll()
                .stream()
                .map(Maintenance::getRepairCost)
                .filter(cost -> cost != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                )
                .doubleValue();
    }
}