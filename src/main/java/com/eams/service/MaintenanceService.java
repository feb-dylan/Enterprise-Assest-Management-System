package com.eams.service;

import com.eams.dto.request.CreateMaintenanceRequest;
import com.eams.dto.request.UpdateMaintenanceStatusRequest;
import com.eams.dto.response.MaintenanceResponse;
import com.eams.entity.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MaintenanceService {
    MaintenanceResponse createMaintenance(CreateMaintenanceRequest request);
    MaintenanceResponse getMaintenanceById(Long id);
    Page<MaintenanceResponse> getAllMaintenance(Pageable pageable);
    Page<MaintenanceResponse> getMaintenanceByAsset(Long assetId, Pageable pageable);
    Page<MaintenanceResponse> getMaintenanceByStatus(MaintenanceStatus status, Pageable pageable);
    MaintenanceResponse updateMaintenanceStatus(Long id, UpdateMaintenanceStatusRequest request);
}