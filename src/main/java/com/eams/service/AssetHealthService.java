package com.eams.service;

import com.eams.dto.response.Responses.AssetHealthResponse;
import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import com.eams.exception.NotFoundException;
import com.eams.repository.AssetRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Asset Health Score (0-100) + "Repair or Replace?" recommendation.
 *
 * Rule-based (no ML needed), built from data the system already has:
 *
 *   age penalty     up to 40 pts   age / expected lifespan of the category
 *   repair count    up to 30 pts   6 pts per repair (max 5)
 *   repair cost     up to 30 pts   total repair cost / purchase price
 *   open damage          10 pts
 *
 * Also computes straight-line depreciation (book value)
 * and total cost of ownership (purchase + repairs).
 */
@Service
@RequiredArgsConstructor
public class AssetHealthService {

    private final AssetRepository assetRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final DamageReportRepository damageReportRepository;

    @Transactional(readOnly = true)
    public AssetHealthResponse forAsset(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found with id: " + assetId));

        long repairs = maintenanceRepository.findByAssetId(assetId).stream()
                .filter(m -> m.getStatus() != com.eams.entity.MaintenanceStatus.CANCELLED)
                .count();
        BigDecimal repairCost = maintenanceRepository.sumRepairCostByAsset(assetId);
        boolean openDamage = damageReportRepository
                .existsByAssetIdAndStatusIn(assetId, DamageReportRepository.OPEN);

        return compute(asset, repairs, repairCost, openDamage);
    }

    /** Health of every non-retired asset, worst first. */
    @Transactional(readOnly = true)
    public List<AssetHealthResponse> all() {
        Map<Long, Object[]> stats = new HashMap<>();
        for (Object[] row : maintenanceRepository.repairStatsPerAsset()) {
            stats.put((Long) row[0], row);
        }

        List<AssetHealthResponse> result = new ArrayList<>();
        for (Asset asset : assetRepository.findAll()) {
            if (asset.getStatus() == AssetStatus.RETIRED) {
                continue;
            }
            Object[] s = stats.get(asset.getId());
            long repairs = s == null ? 0 : ((Number) s[1]).longValue();
            BigDecimal cost = s == null ? BigDecimal.ZERO : toBigDecimal(s[2]);
            boolean openDamage = asset.getStatus() == AssetStatus.DAMAGED
                    || asset.getStatus() == AssetStatus.MAINTENANCE;
            result.add(compute(asset, repairs, cost, openDamage));
        }
        result.sort(Comparator.comparingInt(AssetHealthResponse::score));
        return result;
    }

    public AssetHealthResponse compute(Asset asset, long repairCount,
                                       BigDecimal repairCost, boolean openDamage) {

        int lifespan = asset.getCategory() != null
                && asset.getCategory().getExpectedLifespanMonths() != null
                ? asset.getCategory().getExpectedLifespanMonths()
                : DtoMapper.DEFAULT_LIFESPAN_MONTHS;

        LocalDate start = asset.getPurchaseDate() != null ? asset.getPurchaseDate()
                : (asset.getCreatedAt() != null ? asset.getCreatedAt().toLocalDate() : LocalDate.now());
        long ageMonths = Math.max(0, ChronoUnit.MONTHS.between(start, LocalDate.now()));
        double ageRatio = Math.min(1.0, ageMonths / (double) lifespan);

        BigDecimal price = asset.getPurchasePrice() != null ? asset.getPurchasePrice() : BigDecimal.ZERO;
        BigDecimal cost = repairCost != null ? repairCost : BigDecimal.ZERO;
        double costRatio = price.signum() > 0
                ? cost.divide(price, 4, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        List<String> reasons = new ArrayList<>();
        double penalty = 0;

        double agePenalty = ageRatio * 40;
        penalty += agePenalty;
        reasons.add(String.format("Age %.1f of %.1f expected years (-%d)",
                ageMonths / 12.0, lifespan / 12.0, Math.round(agePenalty)));

        double repairPenalty = Math.min(repairCount, 5) * 6;
        penalty += repairPenalty;
        if (repairCount > 0) {
            reasons.add(repairCount + " repair(s) (-" + Math.round(repairPenalty) + ")");
        }

        double costPenalty = Math.min(1.0, costRatio) * 30;
        penalty += costPenalty;
        if (cost.signum() > 0) {
            reasons.add(String.format("Repairs cost %.0f%% of purchase price (-%d)",
                    costRatio * 100, Math.round(costPenalty)));
        }

        if (openDamage) {
            penalty += 10;
            reasons.add("Has an open damage report (-10)");
        }

        int score = (int) Math.max(0, Math.min(100, Math.round(100 - penalty)));
        String band = score >= 70 ? "GOOD" : score >= 40 ? "FAIR" : "POOR";

        String recommendation;
        if (asset.getStatus() == AssetStatus.RETIRED) {
            recommendation = "RETIRED";
        } else if (score < 40 || costRatio > 0.5 || ageRatio >= 1.0) {
            recommendation = "REPLACE";
            if (costRatio > 0.5) {
                reasons.add("Repairs exceed 50% of the purchase price - replacement is usually cheaper");
            }
            if (ageRatio >= 1.0) {
                reasons.add("Past its expected lifespan");
            }
        } else if (score < 70) {
            recommendation = "MONITOR";
        } else {
            recommendation = "KEEP";
        }

        BigDecimal bookValue = price
                .multiply(BigDecimal.valueOf(Math.max(0.0, 1.0 - ageRatio)))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tco = price.add(cost).setScale(2, RoundingMode.HALF_UP);

        boolean underWarranty = asset.getWarrantyExpiryDate() != null
                && !asset.getWarrantyExpiryDate().isBefore(LocalDate.now());

        return new AssetHealthResponse(
                asset.getId(), asset.getAssetCode(), asset.getName(),
                asset.getCategory() != null ? asset.getCategory().getName() : null,
                score, band, recommendation, reasons,
                Math.round(ageMonths / 12.0 * 10) / 10.0, lifespan, repairCount,
                price, cost.setScale(2, RoundingMode.HALF_UP), bookValue, tco, openDamage,
                asset.getWarrantyExpiryDate(), underWarranty);
    }

    /** Expected yearly repair spend if the asset is kept (repair cost / age in years). */
    public static BigDecimal annualRepairRate(AssetHealthResponse h) {
        double years = Math.max(0.5, h.ageYears());
        return h.totalRepairCost()
                .divide(BigDecimal.valueOf(years), 2, RoundingMode.HALF_UP);
    }

    static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal b) {
            return b;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return BigDecimal.ZERO;
    }
}
