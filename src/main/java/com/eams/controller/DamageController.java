package com.eams.controller;

import com.eams.dto.request.DamageCreateRequest;
import com.eams.dto.request.DamageUpdateRequest;
import com.eams.dto.response.DamageResponse;

import com.eams.service.DamageService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/damage")
@RequiredArgsConstructor
public class DamageController {

    private final DamageService damageService;


    // =========================================================
    // EMPLOYEE REPORTS DAMAGE
    // =========================================================

    @PostMapping
    public ResponseEntity<DamageResponse> createDamageReport(
            @Valid @RequestBody DamageCreateRequest request
    ) {

        DamageResponse response =
                damageService.createDamageReport(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // EMPLOYEE VIEWS OWN REPORTS
    // =========================================================

    @GetMapping("/my/{employeeId}")
    public ResponseEntity<List<DamageResponse>>
    getMyDamageReports(
            @PathVariable Long employeeId
    ) {

        return ResponseEntity.ok(
                damageService.getMyDamageReports(
                        employeeId
                )
        );
    }


    // =========================================================
    // ADMIN / TECHNICIAN VIEW ALL
    // =========================================================

    @GetMapping
    public ResponseEntity<List<DamageResponse>>
    getAllDamageReports() {

        return ResponseEntity.ok(
                damageService.getAllDamageReports()
        );
    }


    // =========================================================
    // VIEW DAMAGE REPORT BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<DamageResponse>
    getDamageReport(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                damageService.getDamageReportById(id)
        );
    }


    // =========================================================
    // VIEW DAMAGE REPORTS FOR ASSET
    // =========================================================

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<DamageResponse>>
    getDamageReportsByAsset(
            @PathVariable Long assetId
    ) {

        return ResponseEntity.ok(
                damageService.getDamageReportsByAsset(
                        assetId
                )
        );
    }


    // =========================================================
    // TECHNICIAN UPDATES DAMAGE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<DamageResponse>
    updateDamageReport(
            @PathVariable Long id,
            @Valid @RequestBody DamageUpdateRequest request
    ) {

        return ResponseEntity.ok(
                damageService.updateDamageReport(
                        id,
                        request
                )
        );
    }
}