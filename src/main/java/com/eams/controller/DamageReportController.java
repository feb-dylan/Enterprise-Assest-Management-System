package com.eams.controller;

import com.eams.constant.AppConstants;
import com.eams.dto.request.CreateDamageReportRequest;
import com.eams.dto.request.UpdateDamageStatusRequest;
import com.eams.dto.response.ApiResponse;
import com.eams.dto.response.DamageReportResponse;
import com.eams.entity.DamageStatus;
import com.eams.service.DamageReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/damage-reports")
@RequiredArgsConstructor
public class DamageReportController {

    private final DamageReportService damageReportService;

    @PostMapping
    public ResponseEntity<ApiResponse<DamageReportResponse>> createDamageReport(@Valid @RequestBody CreateDamageReportRequest request) {
        DamageReportResponse response = damageReportService.createDamageReport(request);
        return new ResponseEntity<>(ApiResponse.success("Damage report filed successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DamageReportResponse>> getDamageReportById(@PathVariable Long id) {
        DamageReportResponse response = damageReportService.getDamageReportById(id);
        return ResponseEntity.ok(ApiResponse.success("Damage report retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DamageReportResponse>>> getAllDamageReports(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) DamageStatus status) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DamageReportResponse> reports;
        if (assetId != null) {
            reports = damageReportService.getDamageReportsByAsset(assetId, pageable);
        } else if (status != null) {
            reports = damageReportService.getDamageReportsByStatus(status, pageable);
        } else {
            reports = damageReportService.getAllDamageReports(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success("Damage reports retrieved successfully", reports));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DamageReportResponse>> updateDamageStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDamageStatusRequest request) {

        DamageReportResponse response = damageReportService.updateDamageStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Damage report status updated successfully", response));
    }
}