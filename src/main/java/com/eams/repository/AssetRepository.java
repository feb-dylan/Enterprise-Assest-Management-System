package com.eams.repository;

import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsByAssetCodeIgnoreCase(String assetCode);

    boolean existsByAssetCodeIgnoreCaseAndIdNot(String assetCode, Long id);

    boolean existsBySerialNumberIgnoreCase(String serialNumber);

    boolean existsBySerialNumberIgnoreCaseAndIdNot(String serialNumber, Long id);

    boolean existsByCategoryId(Long categoryId);

    long countByStatus(AssetStatus status);

    @EntityGraph(attributePaths = "category")
    Optional<Asset> findByAssetCodeIgnoreCase(String assetCode);

    @EntityGraph(attributePaths = "category")
    List<Asset> findByStatus(AssetStatus status);

    @EntityGraph(attributePaths = "category")
    List<Asset> findByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Asset> findByCategoryIdAndStatus(Long categoryId, AssetStatus status);

    @EntityGraph(attributePaths = "category")
    List<Asset> findByNameContainingIgnoreCaseOrAssetCodeContainingIgnoreCase(
            String name, String code);

    @Override
    @EntityGraph(attributePaths = "category")
    List<Asset> findAll();

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Asset> findById(Long id);

    /** Combined filter: every parameter is optional (null = ignore). */
    @EntityGraph(attributePaths = "category")
    @Query("""
        SELECT a FROM Asset a
        WHERE (:status IS NULL OR a.status = :status)
          AND (:categoryId IS NULL OR a.category.id = :categoryId)
          AND (:q IS NULL
               OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.assetCode) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(a.serialNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(a.location, '')) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY a.id DESC
    """)
    List<Asset> filter(
            @Param("status") AssetStatus status,
            @Param("categoryId") Long categoryId,
            @Param("q") String q);

    @EntityGraph(attributePaths = "category")
    @Query("""
        SELECT a FROM Asset a
        WHERE a.warrantyExpiryDate IS NOT NULL
          AND a.warrantyExpiryDate BETWEEN :from AND :to
          AND a.status <> com.eams.entity.AssetStatus.RETIRED
        ORDER BY a.warrantyExpiryDate ASC
    """)
    List<Asset> findWarrantyExpiringBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT COUNT(a) FROM Asset a
        WHERE a.warrantyExpiryDate IS NOT NULL
          AND a.warrantyExpiryDate BETWEEN :from AND :to
          AND a.status <> com.eams.entity.AssetStatus.RETIRED
    """)
    long countWarrantyExpiringBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(a.purchasePrice), 0) FROM Asset a
        WHERE a.status <> com.eams.entity.AssetStatus.RETIRED
    """)
    BigDecimal sumActivePurchasePrice();

    /** [categoryName, count] for the distribution chart. */
    @Query("""
        SELECT a.category.name, COUNT(a) FROM Asset a
        GROUP BY a.category.name
        ORDER BY COUNT(a) DESC
    """)
    List<Object[]> countByCategory();
}
