package com.eams.dto.response;

import com.eams.entity.AuditCampaignStatus;
import com.eams.entity.AuditScanResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response bodies for the new endpoints, grouped in one file.
 */
public final class Responses {

    private Responses() {
    }

    public record MessageResponse(String message) {}

    public record UserResponse(
            Long id,
            String email,
            String role,
            boolean enabled,
            Long employeeId,
            String employeeName,
            LocalDateTime createdAt) {}

    /** Result of scanning an asset QR code: what can THIS user do with it? */
    public record AssetScanResponse(
            AssetResponseDto asset,
            boolean heldByMe,
            Long myAssignmentId,
            List<String> allowedActions) {}

    public record TimelineEvent(
            LocalDateTime at,
            String type,
            String title,
            String description,
            String actor,
            String link,
            String imageUrl) {}

    public record AssetHealthResponse(
            Long assetId,
            String assetCode,
            String assetName,
            String categoryName,
            int score,
            String band,
            String recommendation,
            List<String> reasons,
            double ageYears,
            int expectedLifespanMonths,
            long repairCount,
            BigDecimal purchasePrice,
            BigDecimal totalRepairCost,
            BigDecimal bookValue,
            BigDecimal totalCostOfOwnership,
            boolean openDamage,
            LocalDate warrantyExpiryDate,
            boolean underWarranty) {}

    public record InsightsSummary(
            long totalAssets,
            BigDecimal totalPurchaseValue,
            BigDecimal totalBookValue,
            BigDecimal totalRepairCost,
            long replacementCandidates,
            BigDecimal moneyAtRisk,
            long idleAssets,
            BigDecimal idleAssetValue,
            long reassignments,
            BigDecimal purchasesAvoidedValue,
            long warrantyExpiringIn30Days,
            List<ChartPoint> assetsByCategory,
            List<ChartPoint> repairCostByCategory) {}

    public record IdleAsset(
            Long assetId,
            String assetCode,
            String assetName,
            String categoryName,
            long idleDays,
            BigDecimal purchasePrice,
            long pendingRequestsInCategory) {}

    public record AuditCampaignResponse(
            Long id,
            String name,
            String location,
            AuditCampaignStatus status,
            LocalDateTime createdAt,
            LocalDateTime closedAt,
            long expected,
            long scanned) {}

    public record AuditScanResponse(
            Long id,
            Long assetId,
            String assetCode,
            String assetName,
            AuditScanResult result,
            String expectedLocation,
            String scannedLocation,
            String note,
            String scannedBy,
            LocalDateTime scannedAt) {}

    public record MissingAsset(
            Long assetId,
            String assetCode,
            String assetName,
            String expectedLocation,
            String status) {}

    public record AuditReconciliation(
            AuditCampaignResponse campaign,
            long expected,
            long found,
            long wrongLocation,
            long damaged,
            long missing,
            List<AuditScanResponse> scans,
            List<MissingAsset> missingAssets) {}

    public record AssistantResponse(
            String answer,
            List<String> toolsUsed,
            boolean aiEnabled) {}

    public record AuditLogEntry(
            Long id,
            Long assetId,
            String entityType,
            Long entityId,
            String action,
            String details,
            String actor,
            LocalDateTime at) {}
}
