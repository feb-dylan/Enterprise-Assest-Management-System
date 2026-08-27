package com.eams.service.impl;

import com.eams.dto.request.CreateMaintenanceRequest;
import com.eams.dto.request.UpdateMaintenanceStatusRequest;
import com.eams.dto.response.MaintenanceResponse;
import com.eams.entity.*;
import com.eams.exception.ResourceNotFoundException;
import com.eams.mapper.MaintenanceMapper;
import com.eams.repository.AssetRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.MaintenanceRepository;
import com.eams.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final AssetRepository assetRepository;
    private final DamageReportRepository damageReportRepository;
    private final MaintenanceMapper maintenanceMapper;

    @Override
    @Transactional
    public MaintenanceResponse createMaintenance(CreateMaintenanceRequest request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", request.getAssetId()));

        DamageReport damageReport = null;
        if (request.getDamageReportId() != null) {
            damageReport = damageReportRepository.findById(request.getDamageReportId())
                    .orElseThrow(() -> new ResourceNotFoundException("DamageReport", "id", request.getDamageReportId()));
            damageReport.setStatus(DamageStatus.UNDER_REVIEW);
            damageReportRepository.save(damageReport);
        }

        Maintenance maintenance = maintenanceMapper.toEntity(request);
        maintenance.setAsset(asset);
        maintenance.setDamageReport(damageReport);
        maintenance.setWorkOrderNumber("WO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);

        asset.setStatus(AssetStatus.UNDER_REPAIR);
        assetRepository.save(asset);

        Maintenance savedMaintenance = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponse(savedMaintenance);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponse getMaintenanceById(Long id) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance", "id", id));
        return maintenanceMapper.toResponse(maintenance);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getAllMaintenance(Pageable pageable) {
        return maintenanceRepository.findAll(pageable).map(maintenanceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getMaintenanceByAsset(Long assetId, Pageable pageable) {
        return maintenanceRepository.findByAssetId(assetId, pageable).map(maintenanceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getMaintenanceByStatus(MaintenanceStatus status, Pageable pageable) {
        return maintenanceRepository.findByStatus(status, pageable).map(maintenanceMapper::toResponse);
    }

    @Override
    @Transactional
    public MaintenanceResponse updateMaintenanceStatus(Long id, UpdateMaintenanceStatusRequest request) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance", "id", id));

        maintenance.setStatus(request.getStatus());

        if (request.getCost() != null) {
            maintenance.setCost(request.getCost());
        }
        if (request.getRemarks() != null) {
            maintenance.setRemarks(request.getRemarks());
        }

        if (request.getStatus() == MaintenanceStatus.COMPLETED) {
            maintenance.setCompletionDate(request.getCompletionDate() != null ? request.getCompletionDate() : LocalDate.now());
            maintenance.getAsset().setStatus(AssetStatus.AVAILABLE);
            assetRepository.save(maintenance.getAsset());

            if (maintenance.getDamageReport() != null) {
                maintenance.getDamageReport().setStatus(DamageStatus.REPAIRED);
                damageReportRepository.save(maintenance.getDamageReport());
            }
        } else if (request.getStatus() == MaintenanceStatus.CANCELLED) {
            maintenance.getAsset().setStatus(AssetStatus.AVAILABLE);
            assetRepository.save(maintenance.getAsset());
        }

        Maintenance updated = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponse(updated);
    }
}