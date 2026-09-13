package com.eams.service.impl;

import com.eams.dto.response.ReportResponse;

import com.eams.entity.*;

import com.eams.repository.*;

import com.eams.service.ReportService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl
        implements ReportService {


    private final AssetRepository assetRepository;

    private final AssetRequestRepository
            assetRequestRepository;

    private final DamageReportRepository
            damageReportRepository;

    private final MaintenanceRepository
            maintenanceRepository;

    private final AssetAssignmentRepository
            assetAssignmentRepository;


    // =========================================================
    // ASSET REPORT
    // =========================================================

    @Override
    public ReportResponse getAssetReport() {

        return ReportResponse.builder()

                .reportType("ASSET")

                .total(
                        assetRepository.count()
                )

                .available(
                        countAssets(
                                AssetStatus.AVAILABLE
                        )
                )

                .assigned(
                        countAssets(
                                AssetStatus.ASSIGNED
                        )
                )

                .damaged(
                        countAssets(
                                AssetStatus.DAMAGED
                        )
                )

                .maintenance(
                        countAssets(
                                AssetStatus.MAINTENANCE
                        )
                )

                .retired(
                        countAssets(
                                AssetStatus.RETIRED
                        )
                )

                .build();
    }


    // =========================================================
    // REQUEST REPORT
    // =========================================================

    @Override
    public ReportResponse getRequestReport() {

        return ReportResponse.builder()

                .reportType("ASSET REQUEST")

                .total(
                        assetRequestRepository.count()
                )

                .pending(
                        countRequests(
                                RequestStatus.PENDING
                        )
                )

                .approved(
                        countRequests(
                                RequestStatus.APPROVED
                        )
                )

                .rejected(
                        countRequests(
                                RequestStatus.REJECTED
                        )
                )

                .build();
    }


    // =========================================================
    // MAINTENANCE REPORT
    // =========================================================

    @Override
    public ReportResponse getMaintenanceReport() {

        return ReportResponse.builder()

                .reportType("MAINTENANCE")

                .total(
                        maintenanceRepository.count()
                )

                .completed(
                        countMaintenance(
                                MaintenanceStatus.COMPLETED
                        )
                )

                .inProgress(
                        countMaintenance(
                                MaintenanceStatus.IN_PROGRESS
                        )
                )

                .cancelled(
                        countMaintenance(
                                MaintenanceStatus.CANCELLED
                        )
                )

                .totalRepairCost(
                        calculateRepairCost()
                )

                .build();
    }


    // =========================================================
    // DAMAGE REPORT
    // =========================================================

    @Override
    public ReportResponse getDamageReport() {

        return ReportResponse.builder()

                .reportType("DAMAGE")

                .total(
                        damageReportRepository.count()
                )

                .pending(
                        countDamage(
                                DamageStatus.REPORTED
                        )
                )

                .inProgress(
                        countDamage(
                                DamageStatus.REPAIRING
                        )
                )

                .completed(
                        countDamage(
                                DamageStatus.RESOLVED
                        )
                )

                .build();
    }


    // =========================================================
    // ASSIGNMENT REPORT
    // =========================================================

    @Override
    public ReportResponse getAssignmentReport() {

        return ReportResponse.builder()

                .reportType("ASSIGNMENT")

                .total(
                        assetAssignmentRepository.count()
                )

                .assigned(
                        assetAssignmentRepository
                                .findByStatus(
                                        AssignmentStatus.ACTIVE
                                )
                                .size()
                )

                .completed(
                        assetAssignmentRepository
                                .findByStatus(
                                        AssignmentStatus.RETURNED
                                )
                                .size()
                )

                .build();
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


    private long countRequests(
            RequestStatus status
    ) {

        return assetRequestRepository
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