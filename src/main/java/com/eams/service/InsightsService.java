package com.eams.service;

import com.eams.dto.response.ChartPoint;
import com.eams.dto.response.Responses.AssetHealthResponse;
import com.eams.dto.response.Responses.IdleAsset;
import com.eams.dto.response.Responses.InsightsSummary;
import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssetRequest;
import com.eams.entity.AssetStatus;
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
        Stream.concat(
                requestRepository.findByStatus(RequestStatus.PENDING).stream(),
                requestRepository.findByStatus(RequestStatus.APPROVED).stream())
                .map(AssetRequest::getCategory)
                .filter(c -> c != null)
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
