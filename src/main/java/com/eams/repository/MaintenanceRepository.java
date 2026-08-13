package com.eams.repository;

import com.eams.entity.Maintenance;
import com.eams.entity.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRepository
        extends JpaRepository<Maintenance, Long> {

    List<Maintenance> findByAssetId(Long assetId);

    List<Maintenance> findByStatus(MaintenanceStatus status);
}