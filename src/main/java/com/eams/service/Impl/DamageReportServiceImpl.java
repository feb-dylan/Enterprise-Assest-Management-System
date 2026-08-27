package com.eams.service.impl;

import com.eams.dto.request.CreateDamageReportRequest;
import com.eams.dto.request.UpdateDamageStatusRequest;
import com.eams.dto.response.DamageReportResponse;
import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import com.eams.entity.DamageReport;
import com.eams.entity.DamageStatus;
import com.eams.entity.Employee;
import com.eams.exception.ResourceNotFoundException;
import com.eams.mapper.DamageReportMapper;
import com.eams.repository.AssetRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.EmployeeRepository;
import com.eams.service.DamageReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DamageReportServiceImpl implements DamageReportService {

    private final DamageReportRepository damageReportRepository;
    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final DamageReportMapper damageReportMapper;

    @Override
    @Transactional
    public DamageReportResponse createDamageReport(CreateDamageReportRequest request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", request.getAssetId()));

        Employee employee = employeeRepository.findById(request.getReportedByEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getReportedByEmployeeId()));

        DamageReport damageReport = damageReportMapper.toEntity(request);
        damageReport.setAsset(asset);
        damageReport.setReportedByEmployee(employee);
        damageReport.setStatus(DamageStatus.REPORTED);
        damageReport.setReportNumber("DMG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        asset.setStatus(AssetStatus.DAMAGED);
        assetRepository.save(asset);

        DamageReport savedReport = damageReportRepository.save(damageReport);
        return damageReportMapper.toResponse(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public DamageReportResponse getDamageReportById(Long id) {
        DamageReport report = damageReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DamageReport", "id", id));
        return damageReportMapper.toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DamageReportResponse> getAllDamageReports(Pageable pageable) {
        return damageReportRepository.findAll(pageable).map(damageReportMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DamageReportResponse> getDamageReportsByAsset(Long assetId, Pageable pageable) {
        return damageReportRepository.findByAssetId(assetId, pageable).map(damageReportMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DamageReportResponse> getDamageReportsByStatus(DamageStatus status, Pageable pageable) {
        return damageReportRepository.findByStatus(status, pageable).map(damageReportMapper::toResponse);
    }

    @Override
    @Transactional
    public DamageReportResponse updateDamageStatus(Long id, UpdateDamageStatusRequest request) {
        DamageReport report = damageReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DamageReport", "id", id));

        report.setStatus(request.getStatus());

        if (request.getStatus() == DamageStatus.REPAIRED) {
            report.getAsset().setStatus(AssetStatus.AVAILABLE);
            assetRepository.save(report.getAsset());
        } else if (request.getStatus() == DamageStatus.SCRAPPED) {
            report.getAsset().setStatus(AssetStatus.DISPOSED);
            assetRepository.save(report.getAsset());
        }

        DamageReport updatedReport = damageReportRepository.save(report);
        return damageReportMapper.toResponse(updatedReport);
    }
}