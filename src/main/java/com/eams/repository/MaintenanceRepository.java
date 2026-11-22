package com.eams.repository;

import com.eams.entity.Maintenance;
import com.eams.entity.MaintenanceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaintenanceRepository
        extends JpaRepository<Maintenance, Long> {

    List<MaintenanceStatus> OPEN = List.of(
            MaintenanceStatus.SCHEDULED,
            MaintenanceStatus.IN_PROGRESS);

    @EntityGraph(attributePaths = {"asset", "technicianUser"})
    List<Maintenance> findByAssetId(Long assetId);

    @EntityGraph(attributePaths = {"asset", "technicianUser"})
    List<Maintenance> findByAssetIdOrderByIdAsc(Long assetId);

    List<Maintenance> findByStatus(MaintenanceStatus status);

    List<Maintenance> findByAssetIdAndStatusIn(
            Long assetId, Collection<MaintenanceStatus> statuses);

    boolean existsByAssetIdAndStatusIn(
            Long assetId, Collection<MaintenanceStatus> statuses);

    boolean existsByAssetId(Long assetId);

    long countByStatus(MaintenanceStatus status);

    long countByAssetId(Long assetId);

    @EntityGraph(attributePaths = {"asset", "technicianUser"})
    List<Maintenance> findByTechnicianUserIdOrderByIdDesc(Long userId);

    long countByTechnicianUserIdAndStatus(Long userId, MaintenanceStatus status);

    @Override
    @EntityGraph(attributePaths = {"asset", "technicianUser"})
    List<Maintenance> findAll();

    @Override
    @EntityGraph(attributePaths = {"asset", "technicianUser", "damageReport"})
    Optional<Maintenance> findById(Long id);

    @Query("SELECT COALESCE(SUM(m.repairCost), 0) FROM Maintenance m")
    BigDecimal sumRepairCost();

    @Query("SELECT COALESCE(SUM(m.repairCost), 0) FROM Maintenance m WHERE m.asset.id = :assetId")
    BigDecimal sumRepairCostByAsset(@Param("assetId") Long assetId);

    /** [assetId, repairCount, totalRepairCost] for every asset with maintenance. */
    @Query("""
        SELECT m.asset.id, COUNT(m), COALESCE(SUM(m.repairCost), 0)
        FROM Maintenance m
        WHERE m.status <> com.eams.entity.MaintenanceStatus.CANCELLED
        GROUP BY m.asset.id
    """)
    List<Object[]> repairStatsPerAsset();

    /** [categoryName, totalRepairCost] */
    @Query("""
        SELECT m.asset.category.name, COALESCE(SUM(m.repairCost), 0)
        FROM Maintenance m
        GROUP BY m.asset.category.name
        ORDER BY COALESCE(SUM(m.repairCost), 0) DESC
    """)
    List<Object[]> repairCostByCategory();
}
