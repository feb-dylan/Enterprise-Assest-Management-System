package com.eams.controller;

import com.eams.dto.request.MaintenanceCreateRequest;
import com.eams.dto.request.MaintenanceUpdateRequest;
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


    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    public ResponseEntity<MaintenanceResponse>
    createMaintenance(
            @Valid @RequestBody
            MaintenanceCreateRequest request
    ) {

        MaintenanceResponse response =
                maintenanceService.createMaintenance(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // GET ALL
    // =========================================================

    @GetMapping
    public ResponseEntity<List<MaintenanceResponse>>
    getAllMaintenance() {

        return ResponseEntity.ok(
                maintenanceService.getAllMaintenance()
        );
    }


    // =========================================================
    // GET ONE
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse>
    getMaintenance(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                maintenanceService.getMaintenanceById(
                        id
                )
        );
    }


    // =========================================================
    // GET BY ASSET
    // =========================================================

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<MaintenanceResponse>>
    getMaintenanceByAsset(
            @PathVariable Long assetId
    ) {

        return ResponseEntity.ok(
                maintenanceService.getMaintenanceByAsset(
                        assetId
                )
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceResponse>
    updateMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody
            MaintenanceUpdateRequest request
    ) {

        return ResponseEntity.ok(
                maintenanceService.updateMaintenance(
                        id,
                        request
                )
        );
    }
}