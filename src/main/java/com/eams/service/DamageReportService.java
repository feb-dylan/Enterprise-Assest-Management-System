package com.eams.service;

import com.eams.dto.request.CreateDamageReportRequest;
import com.eams.dto.request.UpdateDamageStatusRequest;
import com.eams.dto.response.DamageReportResponse;
import com.eams.entity.DamageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DamageReportService {
    DamageReportResponse createDamageReport(CreateDamageReportRequest request);
    DamageReportResponse getDamageReportById(Long id);
    Page<DamageReportResponse> getAllDamageReports(Pageable pageable);
    Page<DamageReportResponse> getDamageReportsByAsset(Long assetId, Pageable pageable);
    Page<DamageReportResponse> getDamageReportsByStatus(DamageStatus status, Pageable pageable);
    DamageReportResponse updateDamageStatus(Long id, UpdateDamageStatusRequest request);
}