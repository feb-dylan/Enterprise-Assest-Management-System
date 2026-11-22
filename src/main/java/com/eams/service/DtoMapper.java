package com.eams.service;

import com.eams.dto.response.AssetAssignmentResponse;
import com.eams.dto.response.AssetRequestResponse;
import com.eams.dto.response.AssetResponseDto;
import com.eams.dto.response.CategoryResponseDto;
import com.eams.dto.response.DamageResponse;
import com.eams.dto.response.MaintenanceResponse;
import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssetRequest;
import com.eams.entity.AssignmentStatus;
import com.eams.entity.Category;
import com.eams.entity.DamagePhoto;
import com.eams.entity.DamageReport;
import com.eams.entity.Employee;
import com.eams.entity.Maintenance;
import com.eams.entity.RequestPriority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * All entity -> response mapping in one place
 * (previously duplicated in several services).
 */
@Component
public class DtoMapper {

    public static final int DEFAULT_LIFESPAN_MONTHS = 48;

    public static String fullName(Employee e) {
        if (e == null) {
            return null;
        }
        return (e.getFirstName() + " " + e.getLastName()).trim();
    }

    public CategoryResponseDto category(Category c) {
        if (c == null) {
            return null;
        }
        return CategoryResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .expectedLifespanMonths(c.getExpectedLifespanMonths())
                .build();
    }

    public AssetResponseDto asset(Asset a) {
        return asset(a, null);
    }

    /** @param holder the employee currently holding the asset (optional) */
    public AssetResponseDto asset(Asset a, Employee holder) {
        return AssetResponseDto.builder()
                .id(a.getId())
                .assetCode(a.getAssetCode())
                .name(a.getName())
                .description(a.getDescription())
                .serialNumber(a.getSerialNumber())
                .category(category(a.getCategory()))
                .purchaseDate(a.getPurchaseDate())
                .purchasePrice(a.getPurchasePrice())
                .status(a.getStatus())
                .location(a.getLocation())
                .imageUrl(a.getImageUrl())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .warrantyExpiryDate(a.getWarrantyExpiryDate())
                .vendor(a.getVendor())
                .disposalMethod(a.getDisposalMethod())
                .retiredAt(a.getRetiredAt())
                .currentHolderEmployeeId(holder != null ? holder.getId() : null)
                .currentHolderName(fullName(holder))
                .build();
    }

    public AssetRequestResponse request(AssetRequest r, AssignmentStatus assignmentStatus) {
        Employee e = r.getEmployee();
        Asset a = r.getAsset();
        Category c = r.getCategory() != null ? r.getCategory()
                : (a != null ? a.getCategory() : null);

        return AssetRequestResponse.builder()
                .id(r.getId())
                .employeeId(e != null ? e.getId() : null)
                .employeeCode(e != null ? e.getEmployeeCode() : null)
                .employeeName(fullName(e))
                .assetId(a != null ? a.getId() : null)
                .assetCode(a != null ? a.getAssetCode() : null)
                .assetName(a != null ? a.getName() : null)
                .categoryId(c != null ? c.getId() : null)
                .categoryName(c != null ? c.getName() : null)
                .requestDate(r.getRequestDate())
                .reason(r.getReason())
                .status(r.getStatus())
                .assignmentStatus(assignmentStatus)
                .priority(r.getPriority() != null ? r.getPriority() : RequestPriority.NORMAL)
                .neededBy(r.getNeededBy())
                .approvedBy(r.getApprovedBy() != null ? r.getApprovedBy().getId() : null)
                .approvedByEmail(r.getApprovedBy() != null ? r.getApprovedBy().getEmail() : null)
                .approvedAt(r.getApprovedAt())
                .rejectionReason(r.getRejectionReason())
                .build();
    }

    public AssetAssignmentResponse assignment(AssetAssignment a) {
        Employee e = a.getEmployee();
        return AssetAssignmentResponse.builder()
                .id(a.getId())
                .assetId(a.getAsset().getId())
                .assetCode(a.getAsset().getAssetCode())
                .assetName(a.getAsset().getName())
                .employeeId(e.getId())
                .employeeCode(e.getEmployeeCode())
                .employeeName(fullName(e))
                .assignedDate(a.getAssignedDate())
                .returnedDate(a.getReturnedDate())
                .status(a.getStatus())
                .assignedBy(a.getAssignedBy() != null ? a.getAssignedBy().getId() : null)
                .assignedByEmail(a.getAssignedBy() != null ? a.getAssignedBy().getEmail() : null)
                .returnNote(a.getReturnNote())
                .build();
    }

    public DamageResponse damage(DamageReport r) {
        Asset a = r.getAsset();
        Employee e = r.getEmployee();
        List<String> photos = r.getPhotos() == null ? List.of()
                : r.getPhotos().stream().map(DamagePhoto::getUrl).toList();

        return DamageResponse.builder()
                .id(r.getId())
                .assetId(a != null ? a.getId() : null)
                .assetCode(a != null ? a.getAssetCode() : null)
                .assetName(a != null ? a.getName() : null)
                .employeeId(e != null ? e.getId() : null)
                .employeeCode(e != null ? e.getEmployeeCode() : null)
                .employeeName(fullName(e))
                .description(r.getDescription())
                .reportedDate(r.getReportedDate())
                .evidenceUrl(r.getEvidenceUrl() != null ? r.getEvidenceUrl()
                        : (photos.isEmpty() ? null : photos.get(0)))
                .status(r.getStatus())
                .resolutionNote(r.getResolutionNote())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .photoUrls(photos)
                .aiStatus(r.getAiStatus())
                .aiSeverity(r.getAiSeverity())
                .aiDamageType(r.getAiDamageType())
                .aiSuggestedAction(r.getAiSuggestedAction())
                .aiSummary(r.getAiSummary())
                .aiCostMin(r.getAiCostMin())
                .aiCostMax(r.getAiCostMax())
                .aiSafetyRisk(r.getAiSafetyRisk())
                .aiAnalyzedAt(r.getAiAnalyzedAt())
                .build();
    }

    public MaintenanceResponse maintenance(Maintenance m) {
        Asset a = m.getAsset();
        String technicianName = m.getTechnician();
        if ((technicianName == null || technicianName.isBlank()) && m.getTechnicianUser() != null) {
            technicianName = m.getTechnicianUser().getEmail();
        }
        return MaintenanceResponse.builder()
                .id(m.getId())
                .assetId(a != null ? a.getId() : null)
                .assetCode(a != null ? a.getAssetCode() : null)
                .assetName(a != null ? a.getName() : null)
                .technician(technicianName)
                .description(m.getDescription())
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .repairCost(m.getRepairCost())
                .status(m.getStatus())
                .notes(m.getNotes())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .technicianUserId(m.getTechnicianUser() != null ? m.getTechnicianUser().getId() : null)
                .technicianEmail(m.getTechnicianUser() != null ? m.getTechnicianUser().getEmail() : null)
                .damageReportId(m.getDamageReport() != null ? m.getDamageReport().getId() : null)
                .build();
    }
}
