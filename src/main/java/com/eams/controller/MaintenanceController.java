package com.eams.controller;

import com.eams.constant.AppConstants;
import com.eams.dto.request.CreateMaintenanceRequest;
import com.eams.dto.request.UpdateMaintenanceStatusRequest;
import com.eams.dto.response.ApiResponse;
import com.eams.dto.response.MaintenanceResponse;
import com.eams.entity.MaintenanceStatus;
import com.eams.service.MaintenanceService;
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
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceResponse>> createMaintenance(@Valid @RequestBody CreateMaintenanceRequest request) {
        MaintenanceResponse response = maintenanceService.createMaintenance(request);
        return new ResponseEntity<>(ApiResponse.success("Work order created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> getMaintenanceById(@PathVariable Long id) {
        MaintenanceResponse response = maintenanceService.getMaintenanceById(id);
        return ResponseEntity.ok(ApiResponse.success("Work order retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MaintenanceResponse>>> getAllMaintenance(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String sortDir,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) MaintenanceStatus status) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MaintenanceResponse> result;
        if (assetId != null) {
            result = maintenanceService.getMaintenanceByAsset(assetId, pageable);
        } else if (status != null) {
            result = maintenanceService.getMaintenanceByStatus(status, pageable);
        } else {
            result = maintenanceService.getAllMaintenance(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success("Maintenance records retrieved successfully", result));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> updateMaintenanceStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaintenanceStatusRequest request) {

        MaintenanceResponse response = maintenanceService.updateMaintenanceStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Work order status updated successfully", response));
    }
}