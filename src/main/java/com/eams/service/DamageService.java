package com.eams.service;

import com.eams.dto.request.DamageCreateRequest;
import com.eams.dto.request.DamageUpdateRequest;
import com.eams.dto.response.DamageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DamageService {

    /** The reporter is the logged-in employee; they must currently hold the asset. */
    DamageResponse createDamageReport(DamageCreateRequest request);

    DamageResponse addPhotos(Long damageReportId, List<MultipartFile> files);

    List<DamageResponse> getMyDamageReports(Long employeeIdFromPath);

    List<DamageResponse> getAllDamageReports();

    List<DamageResponse> getDamageReportsByAsset(Long assetId);

    DamageResponse getDamageReportById(Long id);

    DamageResponse updateDamageReport(Long id, DamageUpdateRequest request);

    /** Re-run AI triage (admin / technician). */
    DamageResponse requestAiTriage(Long id);
}
