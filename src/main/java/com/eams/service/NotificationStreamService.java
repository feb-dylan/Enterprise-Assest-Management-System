package com.eams.service;

import com.eams.dto.response.NotificationResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Live notifications over Server-Sent Events.
 *
 * Frontend: open GET /api/notifications/stream with the Authorization header
 * (e.g. using @microsoft/fetch-event-source) and listen for "notification" events.
 */
@Service
public class NotificationStreamService {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId, long unreadCount) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> {
            List<SseEmitter> list = emitters.get(userId);
            if (list != null) {
                list.remove(emitter);
            }
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        try {
            emitter.send(SseEmitter.event().name("connected")
                    .data(Map.of("unreadCount", unreadCount)));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    public void push(Long userId, NotificationResponse notification) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (Exception e) {
                list.remove(emitter);
            }
        }
    }

    /** Keeps proxies from closing idle connections. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.forEach((userId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    list.remove(emitter);
                }
            }
        });
    }

    public int connectionCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }
}
