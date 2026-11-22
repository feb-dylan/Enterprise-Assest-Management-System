package com.eams.repository;

import com.eams.entity.DamageReport;
import com.eams.entity.DamageStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DamageReportRepository
        extends JpaRepository<DamageReport, Long> {

    List<DamageStatus> OPEN = List.of(
            DamageStatus.REPORTED,
            DamageStatus.UNDER_REVIEW,
            DamageStatus.REPAIRING);

    @EntityGraph(attributePaths = {"asset", "employee"})
    List<DamageReport> findByEmployeeIdOrderByIdDesc(Long employeeId);

    List<DamageReport> findByEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = {"asset", "employee"})
    List<DamageReport> findByAssetIdOrderByIdAsc(Long assetId);

    List<DamageReport> findByAssetId(Long assetId);

    @EntityGraph(attributePaths = {"asset", "employee"})
    List<DamageReport> findByStatus(DamageStatus status);

    List<DamageReport> findByAssetIdAndStatus(Long assetId, DamageStatus status);

    List<DamageReport> findByAssetIdAndStatusIn(
            Long assetId, Collection<DamageStatus> statuses);

    boolean existsByAssetIdAndStatusIn(
            Long assetId, Collection<DamageStatus> statuses);

    boolean existsByAssetIdAndStatus(Long assetId, DamageStatus status);

    boolean existsByAssetId(Long assetId);

    boolean existsByEmployeeId(Long employeeId);

    long countByStatus(DamageStatus status);

    long countByEmployeeId(Long employeeId);

    long countByAssetId(Long assetId);

    @Override
    @EntityGraph(attributePaths = {"asset", "employee"})
    List<DamageReport> findAll();

    @Override
    @EntityGraph(attributePaths = {"asset", "employee", "employee.user"})
    Optional<DamageReport> findById(Long id);
}
