package com.eams.repository;

import com.eams.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    /** Used by scheduled jobs so the same alert is not created every day. */
    boolean existsByUserIdAndTypeAndLinkAndCreatedAtAfter(
            Long userId, String type, String link, LocalDateTime after);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllRead(@Param("userId") Long userId);
}
