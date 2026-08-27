package com.eams.repository;

import com.eams.entity.Maintenance;
import com.eams.entity.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    Optional<Maintenance> findByWorkOrderNumber(String workOrderNumber);
    long countByStatus(MaintenanceStatus status);
    Page<Maintenance> findByAssetId(Long assetId, Pageable pageable);
    Page<Maintenance> findByStatus(MaintenanceStatus status, Pageable pageable);
}