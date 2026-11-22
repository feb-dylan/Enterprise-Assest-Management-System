package com.eams.security;

import com.eams.config.AppProperties;
import com.eams.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple brute-force protection: after N failed logins for the same
 * email (default 5), further attempts are refused for M minutes (default 15).
 * In-memory is enough for a single backend instance.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final AppProperties properties;

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    private record Attempts(int count, Instant firstFailure, Instant lockedUntil) {}

    public void checkAllowed(String email) {
        Attempts a = attempts.get(key(email));
        if (a != null && a.lockedUntil() != null && Instant.now().isBefore(a.lockedUntil())) {
            long minutes = Math.max(1,
                    Duration.between(Instant.now(), a.lockedUntil()).toMinutes());
            throw new TooManyRequestsException(
                    "Too many failed login attempts. Try again in " + minutes + " minute(s).");
        }
    }

    public void recordFailure(String email) {
        int max = Math.max(1, properties.auth().maxLoginAttempts());
        Duration window = Duration.ofMinutes(Math.max(1, properties.auth().lockoutMinutes()));
        Instant now = Instant.now();

        attempts.compute(key(email), (k, a) -> {
            if (a == null || a.firstFailure().plus(window).isBefore(now)) {
                a = new Attempts(0, now, null);
            }
            int count = a.count() + 1;
            Instant lockedUntil = count >= max ? now.plus(window) : null;
            return new Attempts(count, a.firstFailure(), lockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
