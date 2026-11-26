package com.dropgate.queue;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final String FLAG_PREFIX = "flagged:";

    private final RateLimitProperties properties;
    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, BucketState> tokenBuckets = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> slidingWindows = new ConcurrentHashMap<>();
    private final Map<String, Long> flaggedFingerprints = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitProperties properties) {
        this(properties, null);
    }

    @Autowired
    public RateLimitService(RateLimitProperties properties, RedisTemplate<String, String> redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public boolean allow(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return true;
        }

        if (isFlagged(fingerprint)) {
            return false;
        }

        return switch (properties.strategy().toLowerCase()) {
            case "sliding-window" -> allowSlidingWindow(fingerprint);
            default -> allowTokenBucket(fingerprint);
        };
    }

    public void flagFingerprint(String fingerprint, long ttlMs) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return;
        }

        long expiresAt = System.currentTimeMillis() + ttlMs;
        flaggedFingerprints.put(fingerprint, expiresAt);

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(FLAG_PREFIX + fingerprint, "1", Duration.ofMillis(ttlMs));
            } catch (RuntimeException ignored) {
                // Keep the local flag active when the shared cache is unavailable.
            }
        }
    }

    private boolean isFlagged(String fingerprint) {
        Long expiresAt = flaggedFingerprints.get(fingerprint);
        if (expiresAt != null && expiresAt <= System.currentTimeMillis()) {
            flaggedFingerprints.remove(fingerprint);
            return false;
        }
        if (expiresAt != null) {
            return true;
        }

        if (redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(FLAG_PREFIX + fingerprint);
                if (value != null) {
                    flaggedFingerprints.put(fingerprint, System.currentTimeMillis() + 30_000L);
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Fall back to the in-process bucket and flag maps during cache outages.
            }
        }

        return false;
    }

    private boolean allowTokenBucket(String fingerprint) {
        BucketState state = tokenBuckets.computeIfAbsent(fingerprint, key -> new BucketState(properties.capacity()));

        synchronized (state) {
            long now = System.currentTimeMillis();
            long elapsedMs = now - state.lastRefillMs;
            if (elapsedMs > 0) {
                double refill = (elapsedMs / 1000.0) * properties.refillPerSecond();
                state.tokens = Math.min(properties.capacity(), state.tokens + refill);
                state.lastRefillMs = now;
            }

            if (state.tokens >= 1.0) {
                state.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    private boolean allowSlidingWindow(String fingerprint) {
        Deque<Long> window = slidingWindows.computeIfAbsent(fingerprint, key -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        long windowMs = properties.windowSeconds() * 1000L;

        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= windowMs) {
                window.pollFirst();
            }

            if (window.size() >= properties.capacity()) {
                return false;
            }

            window.addLast(now);
            return true;
        }
    }

    private static final class BucketState {
        private double tokens;
        private long lastRefillMs;

        private BucketState(int capacity) {
            this.tokens = capacity;
            this.lastRefillMs = System.currentTimeMillis();
        }
    }
}
