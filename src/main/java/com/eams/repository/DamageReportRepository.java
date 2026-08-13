package com.eams.repository;

import com.eams.entity.DamageReport;
import com.eams.entity.DamageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DamageReportRepository
        extends JpaRepository<DamageReport, Long> {

    List<DamageReport> findByEmployeeId(Long employeeId);

    List<DamageReport> findByAssetId(Long assetId);

    List<DamageReport> findByStatus(DamageStatus status);
}