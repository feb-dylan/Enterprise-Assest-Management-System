package com.eams.service;

import com.eams.dto.response.Responses.TimelineEvent;
import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssetRequest;
import com.eams.entity.AuditEvent;
import com.eams.entity.DamageReport;
import com.eams.entity.DamageStatus;
import com.eams.entity.Maintenance;
import com.eams.entity.MaintenanceStatus;
import com.eams.entity.RequestStatus;
import com.eams.exception.NotFoundException;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.AuditEventRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Builds the full life story of one asset from the existing records
 * (requests, assignments, damage reports, maintenance) plus the audit log.
 * Works for old data too, because it reads the original tables.
 */
@Service
@RequiredArgsConstructor
public class TimelineService {

    /** Audit actions already represented by a domain record above. */
    private static final Set<String> AUDIT_ACTIONS_SHOWN = Set.of(
            "STATUS_CHANGED", "RETIRED", "REACTIVATED", "UPDATED",
            "IMAGE_UPLOADED", "AUDIT_SCANNED");

    private final AssetRepository assetRepository;
    private final AssetRequestRepository requestRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final DamageReportRepository damageReportRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AuditEventRepository auditEventRepository;

    @Transactional(readOnly = true)
    public List<TimelineEvent> forAsset(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found with id: " + assetId));

        List<TimelineEvent> events = new ArrayList<>();

        String priceText = asset.getPurchasePrice() != null
                ? " (purchase price " + asset.getPurchasePrice() + ")" : "";
        events.add(new TimelineEvent(asset.getCreatedAt(), "REGISTERED",
                "Registered", asset.getAssetCode() + " - " + asset.getName() + priceText,
                null, "/assets/" + asset.getId(), asset.getImageUrl()));

        for (AssetRequest r : requestRepository.findByAssetIdOrderByIdAsc(assetId)) {
            String who = DtoMapper.fullName(r.getEmployee());
            events.add(new TimelineEvent(r.getRequestDate(), "REQUESTED",
                    "Requested by " + who, r.getReason(), who, "/requests/" + r.getId(), null));
            if (r.getApprovedAt() != null && r.getApprovedBy() != null) {
                boolean rejected = r.getStatus() == RequestStatus.REJECTED;
                events.add(new TimelineEvent(r.getApprovedAt(),
                        rejected ? "REQUEST_REJECTED" : "REQUEST_APPROVED",
                        rejected ? "Request rejected" : "Request approved",
                        rejected ? r.getRejectionReason() : "Request #" + r.getId() + " approved",
                        r.getApprovedBy().getEmail(), "/requests/" + r.getId(), null));
            }
        }

        for (AssetAssignment a : assignmentRepository.findByAssetIdOrderByIdAsc(assetId)) {
            String who = DtoMapper.fullName(a.getEmployee());
            events.add(new TimelineEvent(
                    a.getCreatedAt() != null ? a.getCreatedAt() : atStart(a.getAssignedDate()),
                    "ASSIGNED", "Assigned to " + who, null,
                    a.getAssignedBy() != null ? a.getAssignedBy().getEmail() : null,
                    "/admin/assignments/" + a.getId(), null));
            if (a.getReturnedDate() != null) {
                events.add(new TimelineEvent(
                        a.getUpdatedAt() != null ? a.getUpdatedAt() : atStart(a.getReturnedDate()),
                        "RETURNED", "Returned by " + who, a.getReturnNote(), who,
                        "/admin/assignments/" + a.getId(), null));
            }
        }

        for (DamageReport d : damageReportRepository.findByAssetIdOrderByIdAsc(assetId)) {
            String who = DtoMapper.fullName(d.getEmployee());
            String photo = d.getPhotos().isEmpty() ? d.getEvidenceUrl() : d.getPhotos().get(0).getUrl();
            String ai = d.getAiSeverity() != null
                    ? " [AI: " + d.getAiSeverity() + (d.getAiDamageType() != null
                    ? ", " + d.getAiDamageType() : "") + "]"
                    : "";
            events.add(new TimelineEvent(d.getReportedDate(), "DAMAGE_REPORTED",
                    "Damage reported" + ai, d.getDescription(), who, "/damage/" + d.getId(), photo));
            if (d.getStatus() == DamageStatus.RESOLVED) {
                events.add(new TimelineEvent(d.getUpdatedAt(), "DAMAGE_RESOLVED",
                        "Damage resolved", d.getResolutionNote(), null, "/damage/" + d.getId(), null));
            }
        }

        for (Maintenance m : maintenanceRepository.findByAssetIdOrderByIdAsc(assetId)) {
            String tech = m.getTechnicianUser() != null ? m.getTechnicianUser().getEmail()
                    : m.getTechnician();
            events.add(new TimelineEvent(
                    m.getCreatedAt() != null ? m.getCreatedAt() : atStart(m.getStartDate()),
                    "MAINTENANCE_STARTED", "Maintenance scheduled", m.getDescription(),
                    tech, "/maintenance/" + m.getId(), null));
            if (m.getStatus() == MaintenanceStatus.COMPLETED
                    || m.getStatus() == MaintenanceStatus.CANCELLED) {
                boolean done = m.getStatus() == MaintenanceStatus.COMPLETED;
                String cost = m.getRepairCost() != null ? "Repair cost " + m.getRepairCost() : null;
                events.add(new TimelineEvent(
                        m.getEndDate() != null ? atEnd(m.getEndDate()) : m.getUpdatedAt(),
                        done ? "MAINTENANCE_COMPLETED" : "MAINTENANCE_CANCELLED",
                        done ? "Repair completed" : "Maintenance cancelled",
                        cost, tech, "/maintenance/" + m.getId(), null));
            }
        }

        for (AuditEvent e : auditEventRepository.findByAssetIdOrderByIdAsc(assetId)) {
            if (AUDIT_ACTIONS_SHOWN.contains(e.getAction())) {
                events.add(new TimelineEvent(e.getCreatedAt(), e.getAction(),
                        prettify(e.getAction()), e.getDetails(), e.getActorEmail(), null, null));
            }
        }

        events.removeIf(e -> e.at() == null);
        events.sort(Comparator.comparing(TimelineEvent::at));
        return events;
    }

    private static LocalDateTime atStart(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private static LocalDateTime atEnd(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.of(17, 0));
    }

    private static String prettify(String action) {
        String s = action.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
