package com.eams.service.impl;

import com.eams.dto.request.DamageCreateRequest;
import com.eams.dto.request.DamageUpdateRequest;
import com.eams.dto.response.DamageResponse;

import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import com.eams.entity.DamageReport;
import com.eams.entity.DamageStatus;
import com.eams.entity.Employee;

import com.eams.repository.AssetRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.EmployeeRepository;

import com.eams.service.DamageService;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DamageServiceImpl implements DamageService {

    private final DamageReportRepository damageReportRepository;

    private final EmployeeRepository employeeRepository;

    private final AssetRepository assetRepository;


    // =========================================================
    // CREATE DAMAGE REPORT
    // =========================================================

    @Override
    public DamageResponse createDamageReport(
            DamageCreateRequest request
    ) {

        Employee employee =
                employeeRepository.findById(
                        request.getEmployeeId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found"
                        )
                );


        Asset asset =
                assetRepository.findById(
                        request.getAssetId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Asset not found"
                        )
                );


        if (asset.getStatus() == AssetStatus.RETIRED) {

            throw new RuntimeException(
                    "Cannot report damage for a retired asset"
            );
        }


        DamageReport damageReport =
                new DamageReport();

        damageReport.setEmployee(employee);
        damageReport.setAsset(asset);
        damageReport.setDescription(
                request.getDescription()
        );
        damageReport.setEvidenceUrl(
                request.getEvidenceUrl()
        );
        damageReport.setStatus(
                DamageStatus.REPORTED
        );


        DamageReport saved =
                damageReportRepository.save(
                        damageReport
                );


        // Mark asset as damaged
        asset.setStatus(AssetStatus.DAMAGED);

        assetRepository.save(asset);


        return mapToResponse(saved);
    }


    // =========================================================
    // EMPLOYEE - MY DAMAGE REPORTS
    // =========================================================

    @Override
    public List<DamageResponse> getMyDamageReports(
            Long employeeId
    ) {

        if (!employeeRepository.existsById(employeeId)) {

            throw new RuntimeException(
                    "Employee not found"
            );
        }


        return damageReportRepository
                .findByEmployeeId(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // ADMIN / TECHNICIAN - ALL DAMAGE REPORTS
    // =========================================================

    @Override
    public List<DamageResponse> getAllDamageReports() {

        return damageReportRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // DAMAGE REPORTS BY ASSET
    // =========================================================

    @Override
    public List<DamageResponse> getDamageReportsByAsset(
            Long assetId
    ) {

        if (!assetRepository.existsById(assetId)) {

            throw new RuntimeException(
                    "Asset not found"
            );
        }


        return damageReportRepository
                .findByAssetId(assetId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET ONE DAMAGE REPORT
    // =========================================================

    @Override
    public DamageResponse getDamageReportById(
            Long id
    ) {

        DamageReport report =
                damageReportRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Damage report not found"
                                )
                        );


        return mapToResponse(report);
    }


    // =========================================================
    // TECHNICIAN UPDATES DAMAGE
    // =========================================================

    @Override
    public DamageResponse updateDamageReport(
            Long id,
            DamageUpdateRequest request
    ) {

        DamageReport damageReport =
                damageReportRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Damage report not found"
                                )
                        );


        DamageStatus newStatus =
                request.getStatus();


        damageReport.setStatus(newStatus);

        damageReport.setResolutionNote(
                request.getResolutionNote()
        );


        Asset asset =
                damageReport.getAsset();


        // =====================================================
        // UPDATE ASSET STATUS
        // =====================================================

        switch (newStatus) {

            case REPORTED:
            case UNDER_REVIEW:
                asset.setStatus(
                        AssetStatus.DAMAGED
                );
                break;


            case REPAIRING:
                asset.setStatus(
                        AssetStatus.MAINTENANCE
                );
                break;


            case RESOLVED:

                /*
                 * After damage is resolved, the asset becomes
                 * available again unless it is currently assigned.
                 */

                asset.setStatus(
                        AssetStatus.AVAILABLE
                );

                break;
        }


        assetRepository.save(asset);


        DamageReport saved =
                damageReportRepository.save(
                        damageReport
                );


        return mapToResponse(saved);
    }


    // =========================================================
    // MAPPING
    // =========================================================

    private DamageResponse mapToResponse(
            DamageReport report
    ) {

        Long assetId = null;
        String assetCode = null;
        String assetName = null;

        if (report.getAsset() != null) {

            assetId = report.getAsset().getId();

            assetCode =
                    report.getAsset().getAssetCode();

            assetName =
                    report.getAsset().getName();
        }


        Long employeeId = null;
        String employeeCode = null;
        String employeeName = null;

        if (report.getEmployee() != null) {

            employeeId =
                    report.getEmployee().getId();

            employeeCode =
                    report.getEmployee()
                            .getEmployeeCode();

            employeeName =
                    report.getEmployee()
                            .getFirstName()
                            + " "
                            + report.getEmployee()
                            .getLastName();
        }


        return DamageResponse.builder()

                .id(report.getId())

                .assetId(assetId)

                .assetCode(assetCode)

                .assetName(assetName)

                .employeeId(employeeId)

                .employeeCode(employeeCode)

                .employeeName(employeeName)

                .description(
                        report.getDescription()
                )

                .reportedDate(
                        report.getReportedDate()
                )

                .evidenceUrl(
                        report.getEvidenceUrl()
                )

                .status(
                        report.getStatus()
                )

                .resolutionNote(
                        report.getResolutionNote()
                )

                .createdAt(
                        report.getCreatedAt()
                )

                .updatedAt(
                        report.getUpdatedAt()
                )

                .build();
    }
}