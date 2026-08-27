package com.eams.controller;

import com.eams.dto.request.AssetAssignmentRequest;
import com.eams.dto.request.ReportFilterRequest;
import com.eams.entity.AssetAssignment;
import com.eams.service.AssetAssignmentService;
import com.eams.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final AssetAssignmentService assetAssignmentService;

    // ==========================================
    // 0. ASSET ASSIGNMENT DATA CREATION
    // ==========================================
    @PostMapping("/assignments/create")
    public ResponseEntity<AssetAssignment> createAssetAssignment(@RequestBody AssetAssignmentRequest request) {
        AssetAssignment created = assetAssignmentService.createAssignment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==========================================
    // 1. ASSET INVENTORY REPORTS
    // ==========================================
    @PostMapping("/assets/excel")
    public ResponseEntity<InputStreamResource> exportAssetExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateAssetInventoryExcel(filter), "asset_inventory.xlsx");
    }

    @GetMapping("/assets/excel")
    public ResponseEntity<InputStreamResource> exportAssetExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateAssetInventoryExcel(createFilter(ids)), "asset_inventory.xlsx");
    }

    @PostMapping("/assets/pdf")
    public ResponseEntity<InputStreamResource> exportAssetPdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateAssetInventoryPdf(filter), "asset_inventory.pdf");
    }

    @GetMapping("/assets/pdf")
    public ResponseEntity<InputStreamResource> exportAssetPdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateAssetInventoryPdf(createFilter(ids)), "asset_inventory.pdf");
    }

    // ==========================================
    // 2. EMPLOYEE CUSTODY REPORTS
    // ==========================================
    @PostMapping("/employees/excel")
    public ResponseEntity<InputStreamResource> exportEmployeeExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateEmployeeExcelReport(filter), "employee_custody_report.xlsx");
    }

    @GetMapping("/employees/excel")
    public ResponseEntity<InputStreamResource> exportEmployeeExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateEmployeeExcelReport(createFilter(ids)), "employee_custody_report.xlsx");
    }

    @PostMapping("/employees/pdf")
    public ResponseEntity<InputStreamResource> exportEmployeePdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateEmployeePdfReport(filter), "employee_custody_report.pdf");
    }

    @GetMapping("/employees/pdf")
    public ResponseEntity<InputStreamResource> exportEmployeePdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateEmployeePdfReport(createFilter(ids)), "employee_custody_report.pdf");
    }

    // ==========================================
    // 3. MAINTENANCE LOG & HISTORY REPORTS
    // ==========================================
    @PostMapping("/maintenance/excel")
    public ResponseEntity<InputStreamResource> exportMaintenanceExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateMaintenanceReportExcel(filter), "maintenance_log.xlsx");
    }

    @GetMapping("/maintenance/excel")
    public ResponseEntity<InputStreamResource> exportMaintenanceExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateMaintenanceReportExcel(createFilter(ids)), "maintenance_log.xlsx");
    }

    @PostMapping("/maintenance/pdf")
    public ResponseEntity<InputStreamResource> exportMaintenancePdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateMaintenancePdfReport(filter), "maintenance_history.pdf");
    }

    @GetMapping("/maintenance/pdf")
    public ResponseEntity<InputStreamResource> exportMaintenancePdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateMaintenancePdfReport(createFilter(ids)), "maintenance_history.pdf");
    }

    // ==========================================
    // 4. DAMAGE INCIDENT REPORTS
    // ==========================================
    @PostMapping("/damage/excel")
    public ResponseEntity<InputStreamResource> exportDamageExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateDamageExcelReport(filter), "damage_incidents.xlsx");
    }

    @GetMapping("/damage/excel")
    public ResponseEntity<InputStreamResource> exportDamageExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateDamageExcelReport(createFilter(ids)), "damage_incidents.xlsx");
    }

    @PostMapping("/damage/pdf")
    public ResponseEntity<InputStreamResource> exportDamagePdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateDamagePdfReport(filter), "damage_incidents.pdf");
    }

    @GetMapping("/damage/pdf")
    public ResponseEntity<InputStreamResource> exportDamagePdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateDamagePdfReport(createFilter(ids)), "damage_incidents.pdf");
    }

    // ==========================================
    // 5. DEPARTMENT SUMMARY REPORTS
    // ==========================================
    @PostMapping("/departments/excel")
    public ResponseEntity<InputStreamResource> exportDepartmentExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateDepartmentExcelReport(filter), "department_summary.xlsx");
    }

    @GetMapping("/departments/excel")
    public ResponseEntity<InputStreamResource> exportDepartmentExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateDepartmentExcelReport(createFilter(ids)), "department_summary.xlsx");
    }

    @PostMapping("/departments/pdf")
    public ResponseEntity<InputStreamResource> exportDepartmentPdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateDepartmentPdfReport(filter), "department_summary.pdf");
    }

    @GetMapping("/departments/pdf")
    public ResponseEntity<InputStreamResource> exportDepartmentPdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateDepartmentPdfReport(createFilter(ids)), "department_summary.pdf");
    }

    // ==========================================
    // 6. ASSET ASSIGNMENTS REPORTS
    // ==========================================
    @PostMapping("/assignments/excel")
    public ResponseEntity<InputStreamResource> exportAssignmentsExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateAssetAssignmentsExcelReport(filter), "asset_assignments.xlsx");
    }

    @GetMapping("/assignments/excel")
    public ResponseEntity<InputStreamResource> exportAssignmentsExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateAssetAssignmentsExcelReport(createFilter(ids)), "asset_assignments.xlsx");
    }

    @PostMapping("/assignments/pdf")
    public ResponseEntity<InputStreamResource> exportAssignmentsPdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateAssetAssignmentsPdfReport(filter), "asset_assignments.pdf");
    }

    @GetMapping("/assignments/pdf")
    public ResponseEntity<InputStreamResource> exportAssignmentsPdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateAssetAssignmentsPdfReport(createFilter(ids)), "asset_assignments.pdf");
    }

    // ==========================================
    // 7. ASSET REQUESTS REPORTS
    // ==========================================
    @PostMapping("/requests/excel")
    public ResponseEntity<InputStreamResource> exportRequestsExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateAssetRequestsExcelReport(filter), "asset_requests.xlsx");
    }

    @GetMapping("/requests/excel")
    public ResponseEntity<InputStreamResource> exportRequestsExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateAssetRequestsExcelReport(createFilter(ids)), "asset_requests.xlsx");
    }

    @PostMapping("/requests/pdf")
    public ResponseEntity<InputStreamResource> exportRequestsPdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateAssetRequestsPdfReport(filter), "asset_requests.pdf");
    }

    @GetMapping("/requests/pdf")
    public ResponseEntity<InputStreamResource> exportRequestsPdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateAssetRequestsPdfReport(createFilter(ids)), "asset_requests.pdf");
    }

    // ==========================================
    // 8. CATEGORIES REPORTS
    // ==========================================
    @PostMapping("/categories/excel")
    public ResponseEntity<InputStreamResource> exportCategoriesExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateCategoriesExcelReport(filter), "categories_report.xlsx");
    }

    @GetMapping("/categories/excel")
    public ResponseEntity<InputStreamResource> exportCategoriesExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateCategoriesExcelReport(createFilter(ids)), "categories_report.xlsx");
    }

    @PostMapping("/categories/pdf")
    public ResponseEntity<InputStreamResource> exportCategoriesPdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateCategoriesPdfReport(filter), "categories_report.pdf");
    }

    @GetMapping("/categories/pdf")
    public ResponseEntity<InputStreamResource> exportCategoriesPdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateCategoriesPdfReport(createFilter(ids)), "categories_report.pdf");
    }

    // ==========================================
    // 9. ROLES REPORTS
    // ==========================================
    @PostMapping("/roles/excel")
    public ResponseEntity<InputStreamResource> exportRolesExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateRolesExcelReport(filter), "roles_report.xlsx");
    }

    @GetMapping("/roles/excel")
    public ResponseEntity<InputStreamResource> exportRolesExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateRolesExcelReport(createFilter(ids)), "roles_report.xlsx");
    }

    @PostMapping("/roles/pdf")
    public ResponseEntity<InputStreamResource> exportRolesPdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateRolesPdfReport(filter), "roles_report.pdf");
    }

    @GetMapping("/roles/pdf")
    public ResponseEntity<InputStreamResource> exportRolesPdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateRolesPdfReport(createFilter(ids)), "roles_report.pdf");
    }

    // ==========================================
    // 10. USERS REPORTS
    // ==========================================
    @PostMapping("/users/excel")
    public ResponseEntity<InputStreamResource> exportUsersExcelPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildExcelResponse(reportService.generateUsersExcelReport(filter), "users_report.xlsx");
    }

    @GetMapping("/users/excel")
    public ResponseEntity<InputStreamResource> exportUsersExcelGet(@RequestParam(required = false) List<Long> ids) {
        return buildExcelResponse(reportService.generateUsersExcelReport(createFilter(ids)), "users_report.xlsx");
    }

    @PostMapping("/users/pdf")
    public ResponseEntity<InputStreamResource> exportUsersPdfPost(@RequestBody(required = false) ReportFilterRequest filter) {
        return buildPdfResponse(reportService.generateUsersPdfReport(filter), "users_report.pdf");
    }

    @GetMapping("/users/pdf")
    public ResponseEntity<InputStreamResource> exportUsersPdfGet(@RequestParam(required = false) List<Long> ids) {
        return buildPdfResponse(reportService.generateUsersPdfReport(createFilter(ids)), "users_report.pdf");
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================
    private ReportFilterRequest createFilter(List<Long> ids) {
        ReportFilterRequest filter = new ReportFilterRequest();
        filter.setIds(ids);
        return filter;
    }

    private ResponseEntity<InputStreamResource> buildPdfResponse(ByteArrayInputStream stream, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(stream));
    }

    private ResponseEntity<InputStreamResource> buildExcelResponse(ByteArrayInputStream stream, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(stream));
    }
}