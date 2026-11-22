package com.eams.repository;

import com.eams.entity.AssetRequest;
import com.eams.entity.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssetRequestRepository
        extends JpaRepository<AssetRequest, Long> {

    List<RequestStatus> OPEN = List.of(
            RequestStatus.PENDING,
            RequestStatus.APPROVED);

    @EntityGraph(attributePaths = {"employee", "asset", "category", "approvedBy"})
    List<AssetRequest> findByEmployeeIdOrderByIdDesc(Long employeeId);

    List<AssetRequest> findByEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "asset", "category", "approvedBy"})
    List<AssetRequest> findByStatus(RequestStatus status);

    List<AssetRequest> findByEmployeeIdAndStatus(Long employeeId, RequestStatus status);

    List<AssetRequest> findByAssetIdAndStatusIn(
            Long assetId, Collection<RequestStatus> statuses);

    @EntityGraph(attributePaths = {"employee", "asset", "category", "approvedBy"})
    List<AssetRequest> findByAssetIdOrderByIdAsc(Long assetId);

    boolean existsByEmployeeIdAndAssetIdAndStatusIn(
            Long employeeId, Long assetId, Collection<RequestStatus> statuses);

    boolean existsByAssetId(Long assetId);

    boolean existsByEmployeeId(Long employeeId);

    long countByStatus(RequestStatus status);

    long countByEmployeeIdAndStatus(Long employeeId, RequestStatus status);

    @EntityGraph(attributePaths = {"employee", "asset", "category", "approvedBy"})
    List<AssetRequest> findAllByOrderByIdDesc();

    @Override
    @EntityGraph(attributePaths = {"employee", "employee.user", "asset", "category", "approvedBy"})
    Optional<AssetRequest> findById(Long id);
}
