package com.eams.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only audit log: who did what, to which record, when.
 * Also feeds the asset lifecycle timeline.
 */
@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_asset", columnList = "asset_id"),
                @Index(name = "idx_audit_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Asset this event relates to (null for non-asset events). */
    @Column(name = "asset_id")
    private Long assetId;

    /** ASSET, REQUEST, ASSIGNMENT, DAMAGE, MAINTENANCE, USER, AUDIT */
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    /** STATUS_CHANGED, CREATED, UPDATED, DELETED, RETIRED, ... */
    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 1000)
    private String details;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
