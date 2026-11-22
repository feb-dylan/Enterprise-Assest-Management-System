package com.eams.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_scans",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_audit_scan_campaign_asset",
                columnNames = {"campaign_id", "asset_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class AuditScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AuditCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditScanResult result;

    @Column(name = "scanned_location", length = 255)
    private String scannedLocation;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by")
    private User scannedBy;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @PrePersist
    protected void onCreate() {
        if (scannedAt == null) {
            scannedAt = LocalDateTime.now();
        }
    }
}
