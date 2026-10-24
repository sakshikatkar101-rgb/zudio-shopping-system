package com.zudio.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe In-Memory Login Rate Limiter.
 * Prevents credential brute-forcing by tracking failed attempts per IP and per Email.
 * Maximum 5 failed attempts per 10 minutes.
 */
public class LoginRateLimiter {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long WINDOW_MS = 10L * 60 * 1000; // 10 minutes

    private static class AttemptRecord {
        int count;
        long firstAttemptTime;

        AttemptRecord(int count, long time) {
            this.count = count;
            this.firstAttemptTime = time;
        }
    }

    private static final Map<String, AttemptRecord> ipAttempts = new ConcurrentHashMap<>();
    private static final Map<String, AttemptRecord> emailAttempts = new ConcurrentHashMap<>();

    public static synchronized boolean isBlocked(String ip, String email) {
        long now = System.currentTimeMillis();
        cleanup(ipAttempts, now);
        cleanup(emailAttempts, now);

        if (ip != null && checkBlocked(ipAttempts.get(ip.trim()), now)) {
            return true;
        }
        if (email != null && checkBlocked(emailAttempts.get(email.trim().toLowerCase()), now)) {
            return true;
        }
        return false;
    }

    public static synchronized void recordFailedAttempt(String ip, String email) {
        long now = System.currentTimeMillis();
        if (ip != null && !ip.isBlank()) {
            record(ipAttempts, ip.trim(), now);
        }
        if (email != null && !email.isBlank()) {
            record(emailAttempts, email.trim().toLowerCase(), now);
        }
    }

    public static synchronized void recordSuccess(String ip, String email) {
        if (ip != null) ipAttempts.remove(ip.trim());
        if (email != null) emailAttempts.remove(email.trim().toLowerCase());
    }

    private static boolean checkBlocked(AttemptRecord record, long now) {
        if (record == null) return false;
        if (now - record.firstAttemptTime > WINDOW_MS) {
            return false;
        }
        return record.count >= MAX_FAILED_ATTEMPTS;
    }

    private static void record(Map<String, AttemptRecord> map, String key, long now) {
        AttemptRecord rec = map.get(key);
        if (rec == null || (now - rec.firstAttemptTime > WINDOW_MS)) {
            map.put(key, new AttemptRecord(1, now));
        } else {
            rec.count++;
        }
    }

    private static void cleanup(Map<String, AttemptRecord> map, long now) {
        if (map.size() > 500) {
            map.entrySet().removeIf(e -> now - e.getValue().firstAttemptTime > WINDOW_MS);
        }
    }
}
