package com.eams.controller;

import com.eams.dto.response.ReportResponse;

import com.eams.service.ReportService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;


    // =========================================================
    // ASSET REPORT
    // =========================================================

    @GetMapping("/assets")
    public ResponseEntity<ReportResponse>
    getAssetReport() {

        return ResponseEntity.ok(
                reportService.getAssetReport()
        );
    }


    // =========================================================
    // REQUEST REPORT
    // =========================================================

    @GetMapping("/requests")
    public ResponseEntity<ReportResponse>
    getRequestReport() {

        return ResponseEntity.ok(
                reportService.getRequestReport()
        );
    }


    // =========================================================
    // MAINTENANCE REPORT
    // =========================================================

    @GetMapping("/maintenance")
    public ResponseEntity<ReportResponse>
    getMaintenanceReport() {

        return ResponseEntity.ok(
                reportService.getMaintenanceReport()
        );
    }


    // =========================================================
    // DAMAGE REPORT
    // =========================================================

    @GetMapping("/damage")
    public ResponseEntity<ReportResponse>
    getDamageReport() {

        return ResponseEntity.ok(
                reportService.getDamageReport()
        );
    }


    // =========================================================
    // ASSIGNMENT REPORT
    // =========================================================

    @GetMapping("/assignments")
    public ResponseEntity<ReportResponse>
    getAssignmentReport() {

        return ResponseEntity.ok(
                reportService.getAssignmentReport()
        );
    }
}