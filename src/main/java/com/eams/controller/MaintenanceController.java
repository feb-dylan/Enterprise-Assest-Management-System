package com.eams.controller;

import com.eams.dto.request.MaintenanceCreateRequest;
import com.eams.dto.request.MaintenanceUpdateRequest;
import com.eams.dto.response.MaintenanceAssetResponse;
import com.eams.dto.response.MaintenanceResponse;
import com.eams.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    /** Assets that have a damage report in REPAIRING (ready for a maintenance job). */
    @GetMapping("/repairing-assets")
    public ResponseEntity<List<MaintenanceAssetResponse>> getRepairingAssets() {
        return ResponseEntity.ok(maintenanceService.getRepairingAssets());
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponse> createMaintenance(
            @Valid @RequestBody MaintenanceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceService.createMaintenance(request));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceResponse>> getAllMaintenance() {
        return ResponseEntity.ok(maintenanceService.getAllMaintenance());
    }

    /** The logged-in technician's own job queue. */
    @GetMapping("/my")
    public ResponseEntity<List<MaintenanceResponse>> getMyMaintenance() {
        return ResponseEntity.ok(maintenanceService.getMyMaintenance());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> getMaintenance(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceById(id));
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<MaintenanceResponse>> getMaintenanceByAsset(@PathVariable Long assetId) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceByAsset(assetId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> updateMaintenance(
            @PathVariable Long id, @Valid @RequestBody MaintenanceUpdateRequest request) {
        return ResponseEntity.ok(maintenanceService.updateMaintenance(id, request));
    }
}
