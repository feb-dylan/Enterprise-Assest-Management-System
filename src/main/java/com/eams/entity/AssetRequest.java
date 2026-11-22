package com.eams.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    /**
     * Category-based request ("I need a laptop").
     * The admin picks the concrete asset when assigning.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private RequestPriority priority = RequestPriority.NORMAL;

    @Column(name = "needed_by")
    private LocalDate neededBy;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructor for creating a new asset request
    public AssetRequest(Employee employee, Asset asset, String reason) {
        this.employee = employee;
        this.asset = asset;
        this.category = asset != null ? asset.getCategory() : null;
        this.reason = reason;
        this.status = RequestStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (requestDate == null) {
            requestDate = now;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}