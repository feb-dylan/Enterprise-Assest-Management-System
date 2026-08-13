package com.eams.repository;

import com.eams.entity.AssetRequest;
import com.eams.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRequestRepository
        extends JpaRepository<AssetRequest, Long> {

    List<AssetRequest> findByEmployeeId(Long employeeId);

    List<AssetRequest> findByStatus(RequestStatus status);

    List<AssetRequest> findByEmployeeIdAndStatus(
            Long employeeId,
            RequestStatus status
    );
}