package com.eams.service;

import com.eams.config.AppProperties;
import com.eams.dto.response.NotificationResponse;
import com.eams.entity.Employee;
import com.eams.entity.Notification;
import com.eams.entity.User;
import com.eams.exception.NotFoundException;
import com.eams.repository.NotificationRepository;
import com.eams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * In-app + live (SSE) + email notifications.
 *
 * The notification row is saved in the caller's transaction.
 * The live push and the email are sent only AFTER the transaction commits,
 * so users are never told about a change that was rolled back.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationStreamService streamService;
    private final MailService mailService;
    private final AppProperties properties;

    // ---------------------------------------------------------
    // Sending
    // ---------------------------------------------------------

    public void notify(User user, String type, String title, String message, String link) {
        if (user == null) {
            return;
        }
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(truncate(title, 200));
        n.setMessage(truncate(message, 1000));
        n.setLink(link);
        Notification saved = notificationRepository.save(n);

        Long userId = user.getId();
        String email = user.getEmail();
        NotificationResponse dto = NotificationResponse.from(saved);
        String fullLink = link != null ? properties.frontendUrl() + link : null;

        afterCommit(() -> {
            streamService.push(userId, dto);
            mailService.send(email, "[EAMS] " + title,
                    message + (fullLink != null ? "\n\nOpen: " + fullLink : "") + "\n\n- EAMS");
        });
    }

    public void notifyEmployee(Employee employee, String type, String title,
                               String message, String link) {
        if (employee != null) {
            notify(employee.getUser(), type, title, message, link);
        }
    }

    public void notifyRole(String roleName, String type, String title,
                           String message, String link) {
        for (User user : userRepository.findByRoleName(roleName)) {
            if (user.isEnabled()) {
                notify(user, type, title, message, link);
            }
        }
    }

    // ---------------------------------------------------------
    // Reading (bell menu)
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId, int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        return notificationRepository
                .findByUserIdOrderByIdDesc(userId, PageRequest.of(0, size))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        n.setRead(true);
        return NotificationResponse.from(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    // ---------------------------------------------------------

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    });
        } else {
            action.run();
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
