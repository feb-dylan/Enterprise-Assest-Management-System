package com.eams.service;

import com.eams.dto.request.Requests.AuditCampaignRequest;
import com.eams.dto.request.Requests.AuditScanRequest;
import com.eams.dto.response.Responses.AuditCampaignResponse;
import com.eams.dto.response.Responses.AuditReconciliation;
import com.eams.dto.response.Responses.AuditScanResponse;
import com.eams.dto.response.Responses.MissingAsset;
import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import com.eams.entity.AuditCampaign;
import com.eams.entity.AuditCampaignStatus;
import com.eams.entity.AuditScan;
import com.eams.entity.AuditScanResult;
import com.eams.exception.BadRequestException;
import com.eams.exception.ConflictException;
import com.eams.exception.NotFoundException;
import com.eams.repository.AssetRepository;
import com.eams.repository.AuditCampaignRepository;
import com.eams.repository.AuditScanRepository;
import com.eams.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Physical stock-take ("audit campaign"):
 *  1. Admin starts a campaign (optionally for one location).
 *  2. Staff walk around and scan QR codes; each scan records FOUND / WRONG_LOCATION / DAMAGED.
 *  3. The reconciliation lists every expected asset that was NOT scanned = missing.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuditCampaignService {

    private final AuditCampaignRepository campaignRepository;
    private final AuditScanRepository scanRepository;
    private final AssetRepository assetRepository;
    private final CurrentUserService currentUser;
    private final AuditService auditService;

    public AuditCampaignResponse create(AuditCampaignRequest request) {
        AuditCampaign campaign = new AuditCampaign();
        campaign.setName(request.name().trim());
        campaign.setLocation(request.location() == null || request.location().isBlank()
                ? null : request.location().trim());
        campaign.setCreatedBy(currentUser.user());
        AuditCampaign saved = campaignRepository.save(campaign);
        auditService.record(null, "AUDIT", saved.getId(), "AUDIT_STARTED", saved.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditCampaignResponse> list() {
        return campaignRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    public AuditScanResponse scan(Long campaignId, AuditScanRequest request) {
        AuditCampaign campaign = find(campaignId);
        if (campaign.getStatus() != AuditCampaignStatus.OPEN) {
            throw new BadRequestException("This audit campaign is closed");
        }

        Asset asset = assetRepository.findByAssetCodeIgnoreCase(request.assetCode().trim())
                .orElseThrow(() -> new NotFoundException("No asset with code " + request.assetCode()));
        if (asset.getStatus() == AssetStatus.RETIRED) {
            throw new BadRequestException("Asset " + asset.getAssetCode() + " is retired");
        }

        AuditScanResult result = request.result();
        String scannedLocation = request.scannedLocation() == null || request.scannedLocation().isBlank()
                ? null : request.scannedLocation().trim();
        if (result == null) {
            // Auto-detect: if a location was given and it does not match the record -> WRONG_LOCATION
            result = scannedLocation != null && asset.getLocation() != null
                    && !asset.getLocation().equalsIgnoreCase(scannedLocation)
                    ? AuditScanResult.WRONG_LOCATION
                    : AuditScanResult.FOUND;
        }

        // Scanning the same asset twice updates the previous scan.
        AuditScan scan = scanRepository.findByCampaignIdAndAssetId(campaignId, asset.getId())
                .orElseGet(AuditScan::new);
        scan.setCampaign(campaign);
        scan.setAsset(asset);
        scan.setResult(result);
        scan.setScannedLocation(scannedLocation);
        scan.setNote(request.note());
        scan.setScannedBy(currentUser.user());
        scan.setScannedAt(LocalDateTime.now());
        AuditScan saved = scanRepository.save(scan);

        auditService.record(asset.getId(), "AUDIT", campaignId, "AUDIT_SCANNED",
                campaign.getName() + ": " + result
                        + (scannedLocation != null ? " at " + scannedLocation : ""));
        return toScanResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuditReconciliation reconciliation(Long campaignId) {
        AuditCampaign campaign = find(campaignId);
        List<AuditScan> scans = scanRepository.findByCampaignIdOrderByIdAsc(campaignId);
        Set<Long> scannedIds = scans.stream().map(s -> s.getAsset().getId()).collect(Collectors.toSet());

        List<Asset> expected = expectedAssets(campaign);
        List<MissingAsset> missing = expected.stream()
                .filter(a -> !scannedIds.contains(a.getId()))
                .map(a -> new MissingAsset(a.getId(), a.getAssetCode(), a.getName(),
                        a.getLocation(), a.getStatus().name()))
                .toList();

        long found = scans.stream().filter(s -> s.getResult() == AuditScanResult.FOUND).count();
        long wrong = scans.stream().filter(s -> s.getResult() == AuditScanResult.WRONG_LOCATION).count();
        long damaged = scans.stream().filter(s -> s.getResult() == AuditScanResult.DAMAGED).count();

        return new AuditReconciliation(
                toResponse(campaign), expected.size(), found, wrong, damaged, missing.size(),
                scans.stream().map(this::toScanResponse).toList(), missing);
    }

    public AuditReconciliation close(Long campaignId) {
        AuditCampaign campaign = find(campaignId);
        if (campaign.getStatus() == AuditCampaignStatus.CLOSED) {
            throw new ConflictException("Campaign is already closed");
        }
        campaign.setStatus(AuditCampaignStatus.CLOSED);
        campaign.setClosedAt(LocalDateTime.now());
        AuditReconciliation result = reconciliation(campaignId);
        auditService.record(null, "AUDIT", campaignId, "AUDIT_CLOSED",
                campaign.getName() + ": " + result.found() + " found, "
                        + result.missing() + " missing, " + result.wrongLocation() + " misplaced");
        return result;
    }

    // ---------------------------------------------------------

    private List<Asset> expectedAssets(AuditCampaign campaign) {
        String location = campaign.getLocation() == null ? null
                : campaign.getLocation().toLowerCase(Locale.ROOT);
        return assetRepository.findAll().stream()
                .filter(a -> a.getStatus() != AssetStatus.RETIRED)
                .filter(a -> location == null || (a.getLocation() != null
                        && a.getLocation().toLowerCase(Locale.ROOT).startsWith(location)))
                .toList();
    }

    private AuditCampaignResponse toResponse(AuditCampaign c) {
        return new AuditCampaignResponse(c.getId(), c.getName(), c.getLocation(), c.getStatus(),
                c.getCreatedAt(), c.getClosedAt(),
                expectedAssets(c).size(), scanRepository.countByCampaignId(c.getId()));
    }

    private AuditScanResponse toScanResponse(AuditScan s) {
        return new AuditScanResponse(s.getId(), s.getAsset().getId(), s.getAsset().getAssetCode(),
                s.getAsset().getName(), s.getResult(), s.getAsset().getLocation(),
                s.getScannedLocation(), s.getNote(),
                s.getScannedBy() != null ? s.getScannedBy().getEmail() : null, s.getScannedAt());
    }

    private AuditCampaign find(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Audit campaign not found"));
    }
}
