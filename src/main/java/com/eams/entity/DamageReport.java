package com.eams.entity;

import java.math.BigDecimal;

import java.util.ArrayList;

import java.util.List;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "damage_reports")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class DamageReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "reported_date", nullable = false)
    private LocalDateTime reportedDate;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DamageStatus status = DamageStatus.REPORTED;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @OneToMany(mappedBy = "damageReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DamagePhoto> photos = new ArrayList<>();

    // ----- AI triage (filled in asynchronously after a photo upload) -----

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", length = 20)
    private AiTriageStatus aiStatus;

    @Column(name = "ai_severity", length = 20)
    private String aiSeverity;

    @Column(name = "ai_damage_type", length = 50)
    private String aiDamageType;

    @Column(name = "ai_suggested_action", length = 20)
    private String aiSuggestedAction;

    @Column(name = "ai_summary", length = 1000)
    private String aiSummary;

    @Column(name = "ai_cost_min", precision = 15, scale = 2)
    private BigDecimal aiCostMin;

    @Column(name = "ai_cost_max", precision = 15, scale = 2)
    private BigDecimal aiCostMax;

    @Column(name = "ai_safety_risk")
    private Boolean aiSafetyRisk;

    @Column(name = "ai_analyzed_at")
    private LocalDateTime aiAnalyzedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (reportedDate == null) {
            reportedDate = now;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}