package com.eams.service;

import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import com.eams.entity.DamageStatus;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The ONE place that decides an asset's status.
 *
 * Previously every module (damage, maintenance, assignment) set the
 * status directly, so they overwrote each other - e.g. resolving a
 * damage report made an asset AVAILABLE while an employee still had it.
 *
 * Now the status is DERIVED from the facts in the database, in priority order:
 *
 *   1. RETIRED            - retired assets stay retired
 *   2. MAINTENANCE        - an open maintenance job, or a damage report in REPAIRING
 *   3. DAMAGED            - an open damage report (REPORTED / UNDER_REVIEW)
 *   4. ASSIGNED           - an employee currently holds it (ACTIVE or RETURN_REQUESTED)
 *   5. AVAILABLE          - otherwise
 *
 * Call {@link #refresh(Asset, String)} after any change to assignments,
 * damage reports, or maintenance records.
 */
@Service
@RequiredArgsConstructor
public class AssetLifecycleService {

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final DamageReportRepository damageReportRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AuditService auditService;

    public AssetStatus resolve(Asset asset) {
        if (asset.getStatus() == AssetStatus.RETIRED) {
            return AssetStatus.RETIRED;
        }
        Long id = asset.getId();

        if (maintenanceRepository.existsByAssetIdAndStatusIn(id, MaintenanceRepository.OPEN)
                || damageReportRepository.existsByAssetIdAndStatus(id, DamageStatus.REPAIRING)) {
            return AssetStatus.MAINTENANCE;
        }
        if (damageReportRepository.existsByAssetIdAndStatusIn(id, DamageReportRepository.OPEN)) {
            return AssetStatus.DAMAGED;
        }
        if (assignmentRepository.existsByAssetIdAndStatusIn(id, AssetAssignmentRepository.HOLDING)) {
            return AssetStatus.ASSIGNED;
        }
        return AssetStatus.AVAILABLE;
    }

    /**
     * Recalculates the status, saves it, and writes an audit entry if it changed.
     *
     * @param reason short human-readable cause, shown in the asset timeline
     */
    public AssetStatus refresh(Asset asset, String reason) {
        AssetStatus before = asset.getStatus();
        AssetStatus after = resolve(asset);

        if (before != after) {
            asset.setStatus(after);
            assetRepository.save(asset);
            auditService.record(asset.getId(), "ASSET", asset.getId(),
                    "STATUS_CHANGED",
                    before + " -> " + after + (reason != null ? " (" + reason + ")" : ""));
        }
        return after;
    }

    /** True when an employee currently holds the asset. */
    public boolean isHeld(Long assetId) {
        return assignmentRepository.existsByAssetIdAndStatusIn(
                assetId, AssetAssignmentRepository.HOLDING);
    }

    /** True when the asset has any history that must be preserved. */
    public boolean hasHistory(Long assetId) {
        return assignmentRepository.existsByAssetId(assetId)
                || damageReportRepository.existsByAssetId(assetId)
                || maintenanceRepository.existsByAssetId(assetId);
    }
}
