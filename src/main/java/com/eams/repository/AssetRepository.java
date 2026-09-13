package com.eams.repository;

import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsByAssetCodeIgnoreCase(String assetCode);

    boolean existsByAssetCodeIgnoreCaseAndIdNot(
            String assetCode,
            Long id
    );

    boolean existsBySerialNumberIgnoreCase(String serialNumber);

    boolean existsBySerialNumberIgnoreCaseAndIdNot(
            String serialNumber,
            Long id
    );

    boolean existsByCategoryId(Long categoryId);

    List<Asset> findByStatus(AssetStatus status);

    List<Asset> findByCategoryId(Long categoryId);

    List<Asset> findByNameContainingIgnoreCaseOrAssetCodeContainingIgnoreCase(
            String name,
            String code
    );
}