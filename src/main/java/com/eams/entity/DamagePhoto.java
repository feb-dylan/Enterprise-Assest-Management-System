package com.eams.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "damage_photos")
@Getter
@Setter
@NoArgsConstructor
public class DamagePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "damage_report_id", nullable = false)
    private DamageReport damageReport;

    /** Public URL path, e.g. /uploads/damage/uuid.jpg */
    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
