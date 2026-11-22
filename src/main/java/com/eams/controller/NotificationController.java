package com.eams.controller;

import com.eams.dto.response.NotificationResponse;
import com.eams.security.CurrentUserService;
import com.eams.service.NotificationService;
import com.eams.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/** Bell-menu notifications for the logged-in user. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService streamService;
    private final CurrentUserService currentUser;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(notificationService.list(currentUser.userId(), limit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(currentUser.userId())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(currentUser.userId(), id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllRead(currentUser.userId())));
    }

    /**
     * Live stream (Server-Sent Events). Events:
     *  "connected"    {unreadCount}
     *  "notification" {id, type, title, message, link, read, createdAt}
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Long userId = currentUser.userId();
        return streamService.subscribe(userId, notificationService.unreadCount(userId));
    }
}
