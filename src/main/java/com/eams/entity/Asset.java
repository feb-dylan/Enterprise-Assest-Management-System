package com.eams.entity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "assets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_asset_code",
                        columnNames = "asset_code"
                ),
                @UniqueConstraint(
                        name = "uk_asset_serial_number",
                        columnNames = "serial_number"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "asset_code",
            nullable = false,
            length = 50
    )
    private String assetCode;

    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(
            name = "serial_number",
            unique = true,
            length = 100
    )
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "category_id",
            nullable = false
    )
    private Category category;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(
            name = "purchase_price",
            precision = 15,
            scale = 2
    )
    private BigDecimal purchasePrice;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private AssetStatus status = AssetStatus.AVAILABLE;

    @Column(length = 255)
    private String location;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;

    @Column(length = 150)
    private String vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposal_method", length = 20)
    private DisposalMethod disposalMethod;

    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    /**
     * Optimistic locking: two admins changing the same asset
     * at the same time can no longer silently overwrite each other.
     */
    @Version
    @ColumnDefault("0")
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}