package com.eams.controller;

import com.eams.dto.response.DashboardResponse;
import com.eams.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    public ResponseEntity<DashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/manager")
    public ResponseEntity<DashboardResponse> getManagerDashboard() {
        return ResponseEntity.ok(dashboardService.getManagerDashboard());
    }

    /** /employee (new) or /employee/{ownEmployeeId} (old URL, must be your own id). */
    @GetMapping({"/employee", "/employee/{employeeId}"})
    public ResponseEntity<DashboardResponse> getEmployeeDashboard(
            @PathVariable(required = false) Long employeeId) {
        return ResponseEntity.ok(dashboardService.getEmployeeDashboard(employeeId));
    }

    @GetMapping("/technician")
    public ResponseEntity<DashboardResponse> getTechnicianDashboard() {
        return ResponseEntity.ok(dashboardService.getTechnicianDashboard());
    }
}
