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
    long countByStatus(AssetStatus status);
    Optional<Asset> findByAssetTag(String assetTag);
    boolean existsByAssetTag(String assetTag);
    Page<Asset> findByStatus(AssetStatus status, Pageable pageable);
    Page<Asset> findByCategoryId(Long categoryId, Pageable pageable);
}