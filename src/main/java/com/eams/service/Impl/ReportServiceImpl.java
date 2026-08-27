package com.eams.service.impl;

import com.eams.dto.request.ReportFilterRequest;
import com.eams.entity.*;
import com.eams.repository.*;
import com.eams.service.ReportService;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final DamageReportRepository damageReportRepository;
    private final AssetAssignmentRepository assetAssignmentRepository;
    private final AssetRequestRepository assetRequestRepository;
    private final CategoryRepository categoryRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==========================================
    // 1. ASSET INVENTORY REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateAssetInventoryExcel(ReportFilterRequest filter) {
        List<Asset> assets = getFilteredList(filter, assetRepository);
        String[] headers = {"ID", "Asset Tag", "Name", "Category", "Department", "Status", "Purchase Cost", "Created At"};
        
        return buildExcelReport("Asset Inventory", headers, (sheet, rowIdx) -> {
            for (Asset asset : assets) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(asset.getId());
                row.createCell(1).setCellValue(defaultStr(asset.getAssetTag()));
                row.createCell(2).setCellValue(defaultStr(asset.getName()));
                row.createCell(3).setCellValue(asset.getCategory() != null ? asset.getCategory().getName() : "N/A");
                row.createCell(4).setCellValue(asset.getDepartment() != null ? asset.getDepartment().getName() : "N/A");
                row.createCell(5).setCellValue(asset.getStatus() != null ? asset.getStatus().name() : "");
                row.createCell(6).setCellValue(asset.getPurchaseCost() != null ? asset.getPurchaseCost().doubleValue() : 0.0);
                row.createCell(7).setCellValue(asset.getCreatedAt() != null ? asset.getCreatedAt().format(DATE_FORMATTER) : "");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateAssetInventoryPdf(ReportFilterRequest filter) {
        List<Asset> assets = getFilteredList(filter, assetRepository);
        String[] headers = {"Tag", "Asset Name", "Category", "Department", "Status", "Cost ($)", "Date"};
        float[] widths = {1.5f, 3f, 2.5f, 2.5f, 2f, 2f, 2.5f};

        return buildPdfReport("Asset Inventory Report", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (Asset asset : assets) {
                table.addCell(new Phrase(defaultStr(asset.getAssetTag()), font));
                table.addCell(new Phrase(defaultStr(asset.getName()), font));
                table.addCell(new Phrase(asset.getCategory() != null ? asset.getCategory().getName() : "-", font));
                table.addCell(new Phrase(asset.getDepartment() != null ? asset.getDepartment().getName() : "-", font));
                table.addCell(new Phrase(asset.getStatus() != null ? asset.getStatus().name() : "-", font));
                table.addCell(new Phrase(asset.getPurchaseCost() != null ? "$" + asset.getPurchaseCost() : "$0.00", font));
                table.addCell(new Phrase(asset.getCreatedAt() != null ? asset.getCreatedAt().format(DATE_FORMATTER) : "-", font));
            }
        });
    }

    // ==========================================
    // 2. EMPLOYEE CUSTODY REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateEmployeeExcelReport(ReportFilterRequest filter) {
        List<Employee> employees = getFilteredList(filter, employeeRepository);
        String[] headers = {"ID", "Employee Code", "Full Name", "Email", "Department", "Status"};

        return buildExcelReport("Employee Custody", headers, (sheet, rowIdx) -> {
            for (Employee emp : employees) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(emp.getId());
                row.createCell(1).setCellValue(defaultStr(emp.getEmployeeCode()));
                row.createCell(2).setCellValue(emp.getFirstName() + " " + emp.getLastName());
                row.createCell(3).setCellValue(defaultStr(emp.getEmail()));
                row.createCell(4).setCellValue(emp.getDepartment() != null ? emp.getDepartment().getName() : "N/A");
                row.createCell(5).setCellValue(emp.getStatus() != null ? emp.getStatus().name() : "");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateEmployeePdfReport(ReportFilterRequest filter) {
        List<Employee> employees = getFilteredList(filter, employeeRepository);
        String[] headers = {"Employee Code", "Full Name", "Email", "Department", "Status"};
        float[] widths = {2f, 3f, 4f, 3f, 2f};

        return buildPdfReport("Employee Custody & Asset Summary Report", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (Employee emp : employees) {
                table.addCell(new Phrase(defaultStr(emp.getEmployeeCode()), font));
                table.addCell(new Phrase(emp.getFirstName() + " " + emp.getLastName(), font));
                table.addCell(new Phrase(defaultStr(emp.getEmail()), font));
                table.addCell(new Phrase(emp.getDepartment() != null ? emp.getDepartment().getName() : "-", font));
                table.addCell(new Phrase(emp.getStatus() != null ? emp.getStatus().name() : "-", font));
            }
        });
    }

    // ==========================================
    // 3. MAINTENANCE LOG REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateMaintenanceReportExcel(ReportFilterRequest filter) {
        List<Maintenance> maintenanceList = getFilteredList(filter, maintenanceRepository);
        String[] headers = {"Work Order #", "Asset Tag", "Asset Name", "Technician", "Type", "Cost ($)", "Start Date", "Completion Date", "Status"};

        return buildExcelReport("Maintenance Log", headers, (sheet, rowIdx) -> {
            for (Maintenance m : maintenanceList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(defaultStr(m.getWorkOrderNumber()));
                row.createCell(1).setCellValue(m.getAsset() != null ? defaultStr(m.getAsset().getAssetTag()) : "");
                row.createCell(2).setCellValue(m.getAsset() != null ? defaultStr(m.getAsset().getName()) : "");
                row.createCell(3).setCellValue(defaultStr(m.getTechnicianName()));
                row.createCell(4).setCellValue(defaultStr(m.getMaintenanceType()));
                row.createCell(5).setCellValue(m.getCost() != null ? m.getCost().doubleValue() : 0.0);
                row.createCell(6).setCellValue(m.getStartDate() != null ? m.getStartDate().toString() : "");
                row.createCell(7).setCellValue(m.getCompletionDate() != null ? m.getCompletionDate().toString() : "");
                row.createCell(8).setCellValue(m.getStatus() != null ? m.getStatus().name() : "");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateMaintenancePdfReport(ReportFilterRequest filter) {
        List<Maintenance> maintenanceList = getFilteredList(filter, maintenanceRepository);
        String[] headers = {"Work Order #", "Asset Tag", "Technician", "Type", "Cost ($)", "Status"};
        float[] widths = {2f, 2f, 3f, 3f, 2f, 2f};

        return buildPdfReport("Maintenance History & Work Orders Report", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (Maintenance m : maintenanceList) {
                table.addCell(new Phrase(defaultStr(m.getWorkOrderNumber()), font));
                table.addCell(new Phrase(m.getAsset() != null ? defaultStr(m.getAsset().getAssetTag()) : "-", font));
                table.addCell(new Phrase(defaultStr(m.getTechnicianName()), font));
                table.addCell(new Phrase(defaultStr(m.getMaintenanceType()), font));
                table.addCell(new Phrase(m.getCost() != null ? "$" + m.getCost() : "$0.00", font));
                table.addCell(new Phrase(m.getStatus() != null ? m.getStatus().name() : "-", font));
            }
        });
    }

    // ==========================================
    // 4. DAMAGE INCIDENT REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateDamageExcelReport(ReportFilterRequest filter) {
        List<DamageReport> damageReports = getFilteredList(filter, damageReportRepository);
        String[] headers = {"ID", "Asset Tag", "Reported By", "Description", "Status"};

        return buildExcelReport("Damage Incidents", headers, (sheet, rowIdx) -> {
            for (DamageReport dr : damageReports) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dr.getId());
                row.createCell(1).setCellValue(dr.getAsset() != null ? defaultStr(dr.getAsset().getAssetTag()) : "");
                row.createCell(2).setCellValue(getEmployeeFullName(dr.getReportedByEmployee()));
                row.createCell(3).setCellValue(defaultStr(dr.getDescription()));
                row.createCell(4).setCellValue(dr.getStatus() != null ? dr.getStatus().name() : "");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateDamagePdfReport(ReportFilterRequest filter) {
        List<DamageReport> damageReports = getFilteredList(filter, damageReportRepository);
        String[] headers = {"ID", "Asset Tag", "Reported By", "Description", "Status"};
        float[] widths = {1.5f, 2.5f, 3f, 4f, 2f};

        return buildPdfReport("Damage Incident Reports", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (DamageReport dr : damageReports) {
                table.addCell(new Phrase(String.valueOf(dr.getId()), font));
                table.addCell(new Phrase(dr.getAsset() != null ? defaultStr(dr.getAsset().getAssetTag()) : "-", font));
                table.addCell(new Phrase(getEmployeeFullName(dr.getReportedByEmployee()), font));
                table.addCell(new Phrase(defaultStr(dr.getDescription()), font));
                table.addCell(new Phrase(dr.getStatus() != null ? dr.getStatus().name() : "-", font));
            }
        });
    }

    // ==========================================
    // 5. DEPARTMENT SUMMARY REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateDepartmentExcelReport(ReportFilterRequest filter) {
        List<Department> departments = getFilteredList(filter, departmentRepository);
        String[] headers = {"ID", "Department Name", "Department Code"};

        return buildExcelReport("Department Summary", headers, (sheet, rowIdx) -> {
            for (Department dept : departments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dept.getId());
                row.createCell(1).setCellValue(defaultStr(dept.getName()));
                row.createCell(2).setCellValue(defaultStr(dept.getCode()));
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateDepartmentPdfReport(ReportFilterRequest filter) {
        List<Department> departments = getFilteredList(filter, departmentRepository);
        String[] headers = {"ID", "Department Name", "Department Code"};
        float[] widths = {1f, 3f, 3f};

        return buildPdfReport("Department Asset Valuation & Breakdown", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (Department dept : departments) {
                table.addCell(new Phrase(String.valueOf(dept.getId()), font));
                table.addCell(new Phrase(defaultStr(dept.getName()), font));
                table.addCell(new Phrase(defaultStr(dept.getCode()), font));
            }
        });
    }

    // ==========================================
    // 6. ASSET ASSIGNMENTS REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateAssetAssignmentsExcelReport(ReportFilterRequest filter) {
        List<AssetAssignment> assignments = getFilteredList(filter, assetAssignmentRepository);
        String[] headers = {"ID", "Asset Tag", "Assigned To", "Assigned Date", "Expected Return Date", "Status"};

        return buildExcelReport("Asset Assignments", headers, (sheet, rowIdx) -> {
            for (AssetAssignment assign : assignments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(assign.getId());
                row.createCell(1).setCellValue(assign.getAsset() != null ? defaultStr(assign.getAsset().getAssetTag()) : "");
                row.createCell(2).setCellValue(getEmployeeFullName(assign.getEmployee()));
                row.createCell(3).setCellValue(assign.getAssignedDate() != null ? assign.getAssignedDate().format(DATE_FORMATTER) : "");
                row.createCell(4).setCellValue(assign.getExpectedReturnDate() != null ? assign.getExpectedReturnDate().format(DATE_FORMATTER) : "");
                row.createCell(5).setCellValue(assign.getStatus() != null ? assign.getStatus().name() : "");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateAssetAssignmentsPdfReport(ReportFilterRequest filter) {
        List<AssetAssignment> assignments = getFilteredList(filter, assetAssignmentRepository);
        String[] headers = {"ID", "Asset Tag", "Assigned To", "Assigned Date", "Status"};
        float[] widths = {1f, 2.5f, 3.5f, 2.5f, 2f};

        return buildPdfReport("Asset Assignments Report", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (AssetAssignment assign : assignments) {
                table.addCell(new Phrase(String.valueOf(assign.getId()), font));
                table.addCell(new Phrase(assign.getAsset() != null ? defaultStr(assign.getAsset().getAssetTag()) : "-", font));
                table.addCell(new Phrase(getEmployeeFullName(assign.getEmployee()), font));
                table.addCell(new Phrase(assign.getAssignedDate() != null ? assign.getAssignedDate().format(DATE_FORMATTER) : "-", font));
                table.addCell(new Phrase(assign.getStatus() != null ? assign.getStatus().name() : "-", font));
            }
        });
    }

    // ==========================================
    // 7. ASSET REQUESTS REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateAssetRequestsExcelReport(ReportFilterRequest filter) {
        List<AssetRequest> requests = getFilteredList(filter, assetRequestRepository);
        String[] headers = {"ID", "Requested By", "Category/Asset", "Reason", "Status", "Request Date"};

        return buildExcelReport("Asset Requests", headers, (sheet, rowIdx) -> {
            for (AssetRequest req : requests) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(req.getId());
                row.createCell(1).setCellValue(getEmployeeFullName(req.getEmployee()));
                row.createCell(2).setCellValue(req.getCategory() != null ? req.getCategory().getName() : "N/A");
                row.createCell(3).setCellValue(defaultStr(req.getReason()));
                row.createCell(4).setCellValue(req.getStatus() != null ? req.getStatus().name() : "");
                row.createCell(5).setCellValue(req.getCreatedAt() != null ? req.getCreatedAt().format(DATE_FORMATTER) : "");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateAssetRequestsPdfReport(ReportFilterRequest filter) {
        List<AssetRequest> requests = getFilteredList(filter, assetRequestRepository);
        String[] headers = {"ID", "Requested By", "Category", "Reason", "Status"};
        float[] widths = {1f, 3f, 2.5f, 4f, 2f};

        return buildPdfReport("Asset Requisition & Requests Report", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (AssetRequest req : requests) {
                table.addCell(new Phrase(String.valueOf(req.getId()), font));
                table.addCell(new Phrase(getEmployeeFullName(req.getEmployee()), font));
                table.addCell(new Phrase(req.getCategory() != null ? req.getCategory().getName() : "-", font));
                table.addCell(new Phrase(defaultStr(req.getReason()), font));
                table.addCell(new Phrase(req.getStatus() != null ? req.getStatus().name() : "-", font));
            }
        });
    }

    // ==========================================
    // 8. CATEGORIES REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateCategoriesExcelReport(ReportFilterRequest filter) {
        List<Category> categories = getFilteredList(filter, categoryRepository);
        String[] headers = {"ID", "Category Name", "Description"};

        return buildExcelReport("Asset Categories", headers, (sheet, rowIdx) -> {
            for (Category cat : categories) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(cat.getId());
                row.createCell(1).setCellValue(defaultStr(cat.getName()));
                row.createCell(2).setCellValue(defaultStr(cat.getDescription()));
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateCategoriesPdfReport(ReportFilterRequest filter) {
        List<Category> categories = getFilteredList(filter, categoryRepository);
        String[] headers = {"ID", "Category Name", "Description"};
        float[] widths = {1f, 3f, 5f};

        return buildPdfReport("Asset Categories Report", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (Category cat : categories) {
                table.addCell(new Phrase(String.valueOf(cat.getId()), font));
                table.addCell(new Phrase(defaultStr(cat.getName()), font));
                table.addCell(new Phrase(defaultStr(cat.getDescription()), font));
            }
        });
    }

    // ==========================================
    // 9. ROLES REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateRolesExcelReport(ReportFilterRequest filter) {
        List<Role> roles = getFilteredList(filter, roleRepository);
        String[] headers = {"ID", "Role Name"};

        return buildExcelReport("System Roles", headers, (sheet, rowIdx) -> {
            for (Role role : roles) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(role.getId());
                row.createCell(1).setCellValue(defaultStr(role.getName()));
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateRolesPdfReport(ReportFilterRequest filter) {
        List<Role> roles = getFilteredList(filter, roleRepository);
        String[] headers = {"ID", "Role Name"};
        float[] widths = {1f, 4f};

        return buildPdfReport("System Roles & Permissions Report", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (Role role : roles) {
                table.addCell(new Phrase(String.valueOf(role.getId()), font));
                table.addCell(new Phrase(defaultStr(role.getName()), font));
            }
        });
    }

    // ==========================================
    // 10. USERS REPORTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateUsersExcelReport(ReportFilterRequest filter) {
        List<User> users = getFilteredList(filter, userRepository);
        String[] headers = {"ID", "Username", "Email", "Roles"};

        return buildExcelReport("User Accounts", headers, (sheet, rowIdx) -> {
            for (User user : users) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(defaultStr(user.getUsername()));
                row.createCell(2).setCellValue(defaultStr(user.getEmail()));
                String rolesStr = user.getRoles() != null 
                        ? user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")) 
                        : "N/A";
                row.createCell(3).setCellValue(rolesStr);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream generateUsersPdfReport(ReportFilterRequest filter) {
        List<User> users = getFilteredList(filter, userRepository);
        String[] headers = {"ID", "Username", "Email", "Roles"};
        float[] widths = {1f, 3f, 4f, 3f};

        return buildPdfReport("System Users Directory", headers, widths, table -> {
            com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (User user : users) {
                table.addCell(new Phrase(String.valueOf(user.getId()), font));
                table.addCell(new Phrase(defaultStr(user.getUsername()), font));
                table.addCell(new Phrase(defaultStr(user.getEmail()), font));
                String rolesStr = user.getRoles() != null 
                        ? user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")) 
                        : "-";
                table.addCell(new Phrase(rolesStr, font));
            }
        });
    }

    // ==========================================
    // REUSABLE UTILITY & HELPER METHODS
    // ==========================================

    @FunctionalInterface
    private interface ExcelRowWriter {
        void writeRows(Sheet sheet, int startRowIdx);
    }

    @FunctionalInterface
    private interface PdfTableWriter {
        void populateTable(PdfPTable table);
    }

    private <T> List<T> getFilteredList(ReportFilterRequest filter, org.springframework.data.jpa.repository.JpaRepository<T, Long> repo) {
        return (filter != null && filter.getIds() != null && !filter.getIds().isEmpty())
                ? repo.findAllById(filter.getIds())
                : repo.findAll();
    }

    private ByteArrayInputStream buildExcelReport(String sheetName, String[] headers, ExcelRowWriter writer) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            writer.writeRows(sheet, 1);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    private ByteArrayInputStream buildPdfReport(String titleText, String[] headers, float[] columnWidths, PdfTableWriter writer) {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(columnWidths);

            com.lowagie.text.Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
                cell.setBackgroundColor(new Color(15, 23, 42));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            writer.populateTable(table);

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private String defaultStr(String val) {
        return val != null ? val : "";
    }

    private String getEmployeeFullName(Employee emp) {
        if (emp == null) return "-";
        String fname = emp.getFirstName() != null ? emp.getFirstName() : "";
        String lname = emp.getLastName() != null ? emp.getLastName() : "";
        String full = (fname + " " + lname).trim();
        return full.isEmpty() ? "-" : full;
    }
}