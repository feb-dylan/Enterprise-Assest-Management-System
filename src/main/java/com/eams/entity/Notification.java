package com.eams.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** In-app notification shown in the bell menu. */
@Entity
@Table(
        name = "notifications",
        indexes = @Index(name = "idx_notification_user_read", columnList = "user_id, is_read")
)
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** e.g. REQUEST_APPROVED, ASSET_ASSIGNED, WARRANTY_EXPIRING */
    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String message;

    /** Frontend route to open, e.g. /requests/12 */
    @Column(length = 300)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
