package com.eams.service;

import com.eams.dto.request.MaintenanceCreateRequest;
import com.eams.dto.request.MaintenanceUpdateRequest;
import com.eams.dto.response.MaintenanceAssetResponse;
import com.eams.dto.response.MaintenanceResponse;

import java.util.List;

public interface MaintenanceService {

    MaintenanceResponse createMaintenance(MaintenanceCreateRequest request);

    List<MaintenanceResponse> getAllMaintenance();

    /** Jobs assigned to the logged-in technician. */
    List<MaintenanceResponse> getMyMaintenance();

    MaintenanceResponse getMaintenanceById(Long id);

    List<MaintenanceResponse> getMaintenanceByAsset(Long assetId);

    MaintenanceResponse updateMaintenance(Long id, MaintenanceUpdateRequest request);

    List<MaintenanceAssetResponse> getRepairingAssets();
}
