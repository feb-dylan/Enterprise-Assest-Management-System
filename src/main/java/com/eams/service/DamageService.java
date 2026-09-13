package com.eams.service;

import com.eams.dto.request.DamageCreateRequest;
import com.eams.dto.request.DamageUpdateRequest;
import com.eams.dto.response.DamageResponse;

import java.util.List;

public interface DamageService {

    DamageResponse createDamageReport(
            DamageCreateRequest request
    );

    List<DamageResponse> getMyDamageReports(
            Long employeeId
    );

    List<DamageResponse> getAllDamageReports();

    List<DamageResponse> getDamageReportsByAsset(
            Long assetId
    );

    DamageResponse getDamageReportById(
            Long id
    );

    DamageResponse updateDamageReport(
            Long id,
            DamageUpdateRequest request
    );
}