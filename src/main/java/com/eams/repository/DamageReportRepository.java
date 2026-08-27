package com.eams.repository;

import com.eams.entity.DamageReport;
import com.eams.entity.DamageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {
    Optional<DamageReport> findByReportNumber(String reportNumber);
    long countByStatus(DamageStatus status);
    Page<DamageReport> findByAssetId(Long assetId, Pageable pageable);
    Page<DamageReport> findByStatus(DamageStatus status, Pageable pageable);
}