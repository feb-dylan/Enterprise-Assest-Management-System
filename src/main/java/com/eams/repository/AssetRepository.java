package com.eams.repository;

import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByCategoryId(Long categoryId);
    List<Asset> findByStatus(AssetStatus status);
    Optional<Asset> findByAssetCode(String assetCode);
    boolean existsByAssetCode(String assetCode);
    boolean existsBySerialNumber(String serialNumber);
}