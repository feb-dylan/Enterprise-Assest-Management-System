package com.eams.repository;

import com.eams.entity.AssetAssignment;
import com.eams.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssetAssignmentRepository
        extends JpaRepository<AssetAssignment, Long> {

    /** Assignment states in which the employee still physically has the asset. */
    List<AssignmentStatus> HOLDING = List.of(
            AssignmentStatus.ACTIVE,
            AssignmentStatus.RETURN_REQUESTED);

    @EntityGraph(attributePaths = {"asset", "employee", "assignedBy"})
    List<AssetAssignment> findByEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = {"asset", "employee", "assignedBy"})
    List<AssetAssignment> findByEmployeeIdAndStatusIn(
            Long employeeId, Collection<AssignmentStatus> statuses);

    @EntityGraph(attributePaths = {"asset", "employee", "assignedBy"})
    List<AssetAssignment> findByAssetIdOrderByIdAsc(Long assetId);

    List<AssetAssignment> findByAssetId(Long assetId);

    List<AssetAssignment> findByStatus(AssignmentStatus status);

    long countByStatus(AssignmentStatus status);

    long countByEmployeeIdAndStatus(Long employeeId, AssignmentStatus status);

    long countByAssetId(Long assetId);

    boolean existsByAssetId(Long assetId);

    boolean existsByEmployeeId(Long employeeId);

    boolean existsByAssetIdAndStatus(Long assetId, AssignmentStatus status);

    boolean existsByAssetIdAndStatusIn(
            Long assetId, Collection<AssignmentStatus> statuses);

    @EntityGraph(attributePaths = {"asset", "employee", "employee.user", "employee.department"})
    Optional<AssetAssignment> findFirstByAssetIdAndStatusInOrderByIdDesc(
            Long assetId, Collection<AssignmentStatus> statuses);

    Optional<AssetAssignment> findFirstByEmployeeIdAndAssetIdOrderByIdDesc(
            Long employeeId, Long assetId);

    @Override
    @EntityGraph(attributePaths = {"asset", "employee", "assignedBy"})
    List<AssetAssignment> findAll();

    @Override
    @EntityGraph(attributePaths = {"asset", "employee", "employee.user", "assignedBy"})
    Optional<AssetAssignment> findById(Long id);

    /** Every assignment after an asset's first one = a purchase avoided (Green IT metric). */
    @Query("""
        SELECT COUNT(a) FROM AssetAssignment a
        WHERE EXISTS (SELECT 1 FROM AssetAssignment b
                      WHERE b.asset = a.asset AND b.id < a.id)
    """)
    long countReassignments();

    @Query("""
        SELECT COALESCE(SUM(a.asset.purchasePrice), 0) FROM AssetAssignment a
        WHERE EXISTS (SELECT 1 FROM AssetAssignment b
                      WHERE b.asset = a.asset AND b.id < a.id)
    """)
    BigDecimal sumReassignedPurchaseValue();
}
