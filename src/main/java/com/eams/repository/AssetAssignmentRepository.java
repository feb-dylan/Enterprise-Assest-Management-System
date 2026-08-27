package com.eams.repository;

import com.eams.entity.AssetAssignment;
import com.eams.entity.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, Long> {
    List<AssetAssignment> findByAssetId(Long assetId);
    List<AssetAssignment> findByEmployeeId(Long employeeId);
    Page<AssetAssignment> findByStatus(AssignmentStatus status, Pageable pageable);
}