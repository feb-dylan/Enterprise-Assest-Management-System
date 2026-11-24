package com.eams.service;

import com.eams.dto.response.AssetRequestResponse;
import com.eams.dto.response.ChartPoint;
import com.eams.exception.BadRequestException;
import com.eams.exception.NotFoundException;
import com.eams.repository.EmployeeRepository;
import com.eams.security.CurrentUserService;
import com.eams.dto.response.Responses.AssetHealthResponse;
import com.eams.dto.response.Responses.IdleAsset;
import com.eams.dto.response.Responses.InsightsSummary;
import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssetRequest;
import com.eams.entity.AssetStatus;
import com.eams.entity.RequestPriority;
import com.eams.entity.RequestStatus;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Management insights: replacement candidates, idle assets (Green IT),
 * book value / total cost of ownership, chart data.
 */
@Service
@RequiredArgsConstructor
public class InsightsService {

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetRequestRepository requestRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AssetHealthService healthService;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final DtoMapper dtoMapper;

    @Transactional(readOnly = true)
    public List<AssetHealthResponse> replacementCandidates(int limit) {
        return healthService.all().stream()
                .filter(h -> "REPLACE".equals(h.recommendation()))
                .limit(Math.min(Math.max(limit, 1), 200))
                .toList();
    }

    /**
     * Assets that have been sitting AVAILABLE for at least {@code days} days.
     * Each one shows how many open requests exist for its category -
     * i.e. a purchase that could be avoided by redeploying it.
     */
    @Transactional(readOnly = true)
    public List<IdleAsset> idleAssets(int days) {
        int minDays = Math.max(1, days);
        LocalDate today = LocalDate.now();

        Map<Long, Long> openCategoryRequests = new HashMap<>();
        waitingCategoryRequests()
                .map(AssetRequest::getCategory)
                .forEach(c -> openCategoryRequests.merge(c.getId(), 1L, Long::sum));

        List<IdleAsset> result = new ArrayList<>();
        for (Asset asset : assetRepository.findByStatus(AssetStatus.AVAILABLE)) {
            LocalDate idleSince = assignmentRepository.findByAssetId(asset.getId()).stream()
                    .map(AssetAssignment::getReturnedDate)
                    .filter(d -> d != null)
                    .max(Comparator.naturalOrder())
                    // never assigned: idle since it was bought (or registered)
                    .orElse(asset.getPurchaseDate() != null ? asset.getPurchaseDate()
                            : asset.getCreatedAt() != null ? asset.getCreatedAt().toLocalDate()
                            : today);
            long idleDays = ChronoUnit.DAYS.between(idleSince, today);
            if (idleDays >= minDays) {
                result.add(new IdleAsset(asset.getId(), asset.getAssetCode(), asset.getName(),
                        asset.getCategory() != null ? asset.getCategory().getName() : null,
                        idleDays, asset.getPurchasePrice(),
                        asset.getCategory() != null
                                ? openCategoryRequests.getOrDefault(asset.getCategory().getId(), 0L)
                                : 0L));
            }
        }
        result.sort(Comparator.comparingLong(IdleAsset::idleDays).reversed());
        return result;
    }

    /** "Any item of a category" requests that still wait for an asset (pending or approved). */
    private Stream<AssetRequest> waitingCategoryRequests() {
        return Stream.concat(
                        requestRepository.findByStatus(RequestStatus.PENDING).stream(),
                        requestRepository.findByStatus(RequestStatus.APPROVED).stream())
                .filter(r -> r.getCategory() != null);
    }

    /** The waiting requests this idle asset could fill (same category), most urgent first. */
    @Transactional(readOnly = true)
    public List<AssetRequestResponse> waitingRequestsFor(Long assetId) {
        Asset asset = findAsset(assetId);
        if (asset.getCategory() == null) {
            return List.of();
        }
        Long categoryId = asset.getCategory().getId();
        return waitingCategoryRequests()
                .filter(r -> r.getCategory().getId().equals(categoryId))
                .sorted(Comparator.comparing((AssetRequest r) -> r.getPriority() != null
                                ? r.getPriority() : RequestPriority.NORMAL).reversed()
                        .thenComparing(AssetRequest::getRequestDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(r -> dtoMapper.request(r, null))
                .toList();
    }

    /**
     * An admin asks the managers to approve one of the PENDING requests this idle asset
     * could fill (the admin can only assign after a manager approves).
     * Every manager gets a notification (bell, live) that opens "Pending Requests".
     */
    @Transactional
    public void remindManagers(Long assetId, String note) {
        Asset asset = findAsset(assetId);
        long pending = waitingRequestsFor(assetId).stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .count();
        if (pending == 0) {
            throw new BadRequestException("No requests are waiting for manager approval.");
        }

        String sender = employeeRepository.findByUserId(currentUser.userId())
                .map(DtoMapper::fullName)
                .orElse(currentUser.principal().email());

        StringBuilder message = new StringBuilder()
                .append(sender).append(" (admin): ")
                .append(asset.getAssetCode()).append(" - ").append(asset.getName())
                .append(" is unused in the store, and ").append(pending)
                .append(" request(s) for a ").append(asset.getCategory().getName())
                .append(" are waiting for your approval. ")
                .append("Please approve one, then I can assign this asset instead of buying a new one.");
        if (note != null && !note.isBlank()) {
            message.append(" Note: ").append(note.trim());
        }

        notificationService.notifyRole("MANAGER", "IDLE_ASSET_REMINDER",
                "Please approve a request for idle " + asset.getAssetCode(),
                message.toString(), "/requests/pending");
        auditService.record(asset.getId(), "ASSET", asset.getId(), "REMINDER_SENT",
                sender + " reminded managers about " + pending + " pending request(s)");
    }

    private Asset findAsset(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found with id: " + assetId));
    }

    @Transactional(readOnly = true)
    public InsightsSummary summary() {
        List<AssetHealthResponse> health = healthService.all();

        BigDecimal purchaseValue = BigDecimal.ZERO;
        BigDecimal bookValue = BigDecimal.ZERO;
        BigDecimal moneyAtRisk = BigDecimal.ZERO;
        long candidates = 0;
        for (AssetHealthResponse h : health) {
            purchaseValue = purchaseValue.add(h.purchasePrice());
            bookValue = bookValue.add(h.bookValue());
            if ("REPLACE".equals(h.recommendation())) {
                candidates++;
                moneyAtRisk = moneyAtRisk.add(AssetHealthService.annualRepairRate(h));
            }
        }

        List<IdleAsset> idle = idleAssets(90);
        BigDecimal idleValue = idle.stream()
                .map(IdleAsset::purchasePrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();

        return new InsightsSummary(
                assetRepository.count(),
                purchaseValue,
                bookValue,
                maintenanceRepository.sumRepairCost(),
                candidates,
                moneyAtRisk,
                idle.size(),
                idleValue,
                assignmentRepository.countReassignments(),
                assignmentRepository.sumReassignedPurchaseValue(),
                assetRepository.countWarrantyExpiringBetween(today, today.plusDays(30)),
                assetsByCategory(),
                repairCostByCategory());
    }

    public List<ChartPoint> assetsByCategory() {
        return assetRepository.countByCategory().stream()
                .map(r -> ChartPoint.of(r[0], r[1]))
                .toList();
    }

    public List<ChartPoint> repairCostByCategory() {
        return maintenanceRepository.repairCostByCategory().stream()
                .map(r -> ChartPoint.of(r[0], r[1]))
                .toList();
    }
}
