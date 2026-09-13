package com.eams.controller;

import com.eams.dto.response.DashboardResponse;

import com.eams.service.DashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;


    // =========================================================
    // ADMIN
    // =========================================================

    @GetMapping("/admin")
    public ResponseEntity<DashboardResponse>
    getAdminDashboard() {

        return ResponseEntity.ok(
                dashboardService.getAdminDashboard()
        );
    }


    // =========================================================
    // MANAGER
    // =========================================================

    @GetMapping("/manager")
    public ResponseEntity<DashboardResponse>
    getManagerDashboard() {

        return ResponseEntity.ok(
                dashboardService.getManagerDashboard()
        );
    }


    // =========================================================
    // EMPLOYEE
    // =========================================================

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<DashboardResponse>
    getEmployeeDashboard(
            @PathVariable Long employeeId
    ) {

        return ResponseEntity.ok(
                dashboardService.getEmployeeDashboard(
                        employeeId
                )
        );
    }


    // =========================================================
    // TECHNICIAN
    // =========================================================

    @GetMapping("/technician")
    public ResponseEntity<DashboardResponse>
    getTechnicianDashboard() {

        return ResponseEntity.ok(
                dashboardService.getTechnicianDashboard()
        );
    }
}