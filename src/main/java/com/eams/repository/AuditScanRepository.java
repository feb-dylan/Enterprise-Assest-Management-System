package com.eams.repository;

import com.eams.entity.AuditScan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditScanRepository
        extends JpaRepository<AuditScan, Long> {

    @EntityGraph(attributePaths = {"asset", "scannedBy"})
    List<AuditScan> findByCampaignIdOrderByIdAsc(Long campaignId);

    Optional<AuditScan> findByCampaignIdAndAssetId(Long campaignId, Long assetId);

    long countByCampaignId(Long campaignId);
}
