package com.eams.service;

import com.eams.dto.request.ReportFilterRequest;
import java.io.ByteArrayInputStream;

public interface ReportService {

    // 1. Asset Inventory
    ByteArrayInputStream generateAssetInventoryExcel(ReportFilterRequest filter);
    ByteArrayInputStream generateAssetInventoryPdf(ReportFilterRequest filter);

    // 2. Employee Custody
    ByteArrayInputStream generateEmployeeExcelReport(ReportFilterRequest filter);
    ByteArrayInputStream generateEmployeePdfReport(ReportFilterRequest filter);

    // 3. Maintenance Log & History
    ByteArrayInputStream generateMaintenanceReportExcel(ReportFilterRequest filter);
    ByteArrayInputStream generateMaintenancePdfReport(ReportFilterRequest filter);

    // 4. Damage Incidents
    ByteArrayInputStream generateDamageExcelReport(ReportFilterRequest filter);
    ByteArrayInputStream generateDamagePdfReport(ReportFilterRequest filter);

    // 5. Department Summary
    ByteArrayInputStream generateDepartmentExcelReport(ReportFilterRequest filter);
    ByteArrayInputStream generateDepartmentPdfReport(ReportFilterRequest filter);

    // 6. Asset Assignments
    ByteArrayInputStream generateAssetAssignmentsExcelReport(ReportFilterRequest filter);
    ByteArrayInputStream generateAssetAssignmentsPdfReport(ReportFilterRequest filter);

    // 7. Asset Requests
    ByteArrayInputStream generateAssetRequestsExcelReport(ReportFilterRequest filter);
    ByteArrayInputStream generateAssetRequestsPdfReport(ReportFilterRequest filter);

    // 8. Categories
    ByteArrayInputStream generateCategoriesExcelReport(ReportFilterRequest filter);
    ByteArrayInputStream generateCategoriesPdfReport(ReportFilterRequest filter);

    // 9. Roles
    ByteArrayInputStream generateRolesExcelReport(ReportFilterRequest filter);
    ByteArrayInputStream generateRolesPdfReport(ReportFilterRequest filter);

    // 10. Users
    ByteArrayInputStream generateUsersExcelReport(ReportFilterRequest filter);
    ByteArrayInputStream generateUsersPdfReport(ReportFilterRequest filter);
}