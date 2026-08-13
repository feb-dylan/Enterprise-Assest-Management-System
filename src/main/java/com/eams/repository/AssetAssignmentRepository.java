package com.eams.repository;

import com.eams.entity.AssetAssignment;
import com.eams.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetAssignmentRepository
        extends JpaRepository<AssetAssignment, Long> {

    List<AssetAssignment> findByEmployeeId(Long employeeId);

    List<AssetAssignment> findByAssetId(Long assetId);

    List<AssetAssignment> findByStatus(AssignmentStatus status);

    boolean existsByAssetIdAndStatus(
            Long assetId,
            AssignmentStatus status
    );
}