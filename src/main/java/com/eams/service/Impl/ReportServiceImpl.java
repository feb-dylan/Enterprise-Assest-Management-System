package com.eams.service.impl;

import com.eams.dto.response.ReportResponse;
import com.eams.entity.AssetStatus;
import com.eams.entity.AssignmentStatus;
import com.eams.entity.DamageStatus;
import com.eams.entity.MaintenanceStatus;
import com.eams.entity.RequestStatus;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.MaintenanceRepository;
import com.eams.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AssetRepository assetRepository;
    private final AssetRequestRepository requestRepository;
    private final DamageReportRepository damageReportRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AssetAssignmentRepository assignmentRepository;

    @Override
    public ReportResponse getAssetReport() {
        return ReportResponse.builder()
                .reportType("ASSET")
                .total(assetRepository.count())
                .available(assetRepository.countByStatus(AssetStatus.AVAILABLE))
                .assigned(assetRepository.countByStatus(AssetStatus.ASSIGNED))
                .damaged(assetRepository.countByStatus(AssetStatus.DAMAGED))
                .maintenance(assetRepository.countByStatus(AssetStatus.MAINTENANCE))
                .retired(assetRepository.countByStatus(AssetStatus.RETIRED))
                .build();
    }

    @Override
    public ReportResponse getRequestReport() {
        return ReportResponse.builder()
                .reportType("ASSET REQUEST")
                .total(requestRepository.count())
                .pending(requestRepository.countByStatus(RequestStatus.PENDING))
                .approved(requestRepository.countByStatus(RequestStatus.APPROVED))
                .rejected(requestRepository.countByStatus(RequestStatus.REJECTED))
                .completed(requestRepository.countByStatus(RequestStatus.ASSIGNED))
                .build();
    }

    @Override
    public ReportResponse getMaintenanceReport() {
        return ReportResponse.builder()
                .reportType("MAINTENANCE")
                .total(maintenanceRepository.count())
                .pending(maintenanceRepository.countByStatus(MaintenanceStatus.SCHEDULED))
                .inProgress(maintenanceRepository.countByStatus(MaintenanceStatus.IN_PROGRESS))
                .completed(maintenanceRepository.countByStatus(MaintenanceStatus.COMPLETED))
                .cancelled(maintenanceRepository.countByStatus(MaintenanceStatus.CANCELLED))
                .totalRepairCost(maintenanceRepository.sumRepairCost())
                .build();
    }

    @Override
    public ReportResponse getDamageReport() {
        return ReportResponse.builder()
                .reportType("DAMAGE")
                .total(damageReportRepository.count())
                .pending(damageReportRepository.countByStatus(DamageStatus.REPORTED)
                        + damageReportRepository.countByStatus(DamageStatus.UNDER_REVIEW))
                .inProgress(damageReportRepository.countByStatus(DamageStatus.REPAIRING))
                .completed(damageReportRepository.countByStatus(DamageStatus.RESOLVED))
                .build();
    }

    @Override
    public ReportResponse getAssignmentReport() {
        return ReportResponse.builder()
                .reportType("ASSIGNMENT")
                .total(assignmentRepository.count())
                .assigned(assignmentRepository.countByStatus(AssignmentStatus.ACTIVE))
                .pending(assignmentRepository.countByStatus(AssignmentStatus.RETURN_REQUESTED))
                .completed(assignmentRepository.countByStatus(AssignmentStatus.RETURNED))
                .build();
    }
}
