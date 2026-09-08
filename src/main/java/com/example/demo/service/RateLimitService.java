package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory fixed-window rate limiter for auth endpoints
 * (login, Google login, registration) to blunt credential-stuffing
 * and registration-spam attempts. Single-instance only - if this
 * service is ever horizontally scaled, back this with Redis instead.
 */
@Service
public class RateLimitService {

    private record Window(AtomicInteger count, long windowStartEpochSeconds) {}

    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

    /**
     * @param key         identifies the caller+action, e.g. "login:203.0.113.4"
     * @param maxAttempts allowed attempts per window
     * @param windowSeconds window length in seconds
     * @return true if the attempt is allowed, false if the caller should be rejected (HTTP 429)
     */
    public boolean tryAcquire(String key, int maxAttempts, int windowSeconds) {
        long now = Instant.now().getEpochSecond();
        Window window = attempts.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartEpochSeconds() >= windowSeconds) {
                return new Window(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return window.count().get() <= maxAttempts;
    }

    /** Test-only hook to reset all counters between test cases sharing the Spring context. */
    public void clearAll() {
        attempts.clear();
    }
}
