package com.eams.service.impl;

import com.eams.dto.response.DashboardSummaryResponse;
import com.eams.entity.AssetStatus;
import com.eams.entity.DamageStatus;
import com.eams.entity.Maintenance;
import com.eams.entity.MaintenanceStatus;
import com.eams.repository.*;
import com.eams.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final DamageReportRepository damageReportRepository;
    private final MaintenanceRepository maintenanceRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {

        // Asset Counts
        long totalAssets = assetRepository.count();
        long availableAssets = assetRepository.countByStatus(AssetStatus.AVAILABLE);
        long assignedAssets = assetRepository.countByStatus(AssetStatus.ASSIGNED);
        long underRepairAssets = assetRepository.countByStatus(AssetStatus.UNDER_REPAIR);
        long damagedAssets = assetRepository.countByStatus(AssetStatus.DAMAGED);

        // Org Counts
        long totalEmployees = employeeRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalCategories = categoryRepository.count();

        // Active Issues
        long pendingDamageReports = damageReportRepository.countByStatus(DamageStatus.REPORTED);
        long activeMaintenances = maintenanceRepository.countByStatus(MaintenanceStatus.SCHEDULED)
                + maintenanceRepository.countByStatus(MaintenanceStatus.IN_PROGRESS);

        // Maintenance Cost Calculation
        BigDecimal totalMaintenanceCost = maintenanceRepository.findAll().stream()
                .map(Maintenance::getCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardSummaryResponse.builder()
                .totalAssets(totalAssets)
                .availableAssets(availableAssets)
                .assignedAssets(assignedAssets)
                .underRepairAssets(underRepairAssets)
                .damagedAssets(damagedAssets)
                .totalEmployees(totalEmployees)
                .totalDepartments(totalDepartments)
                .totalCategories(totalCategories)
                .pendingDamageReports(pendingDamageReports)
                .activeMaintenances(activeMaintenances)
                .totalMaintenanceCost(totalMaintenanceCost)
                .build();
    }
}