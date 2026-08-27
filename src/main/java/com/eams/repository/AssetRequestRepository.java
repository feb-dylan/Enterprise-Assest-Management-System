package com.eams.repository;

import com.eams.entity.AssetRequest;
import com.eams.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRequestRepository extends JpaRepository<AssetRequest, Long> {
    Optional<AssetRequest> findByRequestNumber(String requestNumber);
    Page<AssetRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    Page<AssetRequest> findByStatus(RequestStatus status, Pageable pageable);
}