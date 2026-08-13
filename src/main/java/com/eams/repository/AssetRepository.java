package com.eams.repository;

import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByAssetCode(String assetCode);

    boolean existsByAssetCode(String assetCode);

    List<Asset> findByCategoryId(Long categoryId);

    List<Asset> findByStatus(AssetStatus status);

    List<Asset> findByNameContainingIgnoreCase(String name);
}