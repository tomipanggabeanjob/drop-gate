package com.dropgate.queue;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dropgate.queue.rate-limit")
public record RateLimitProperties(
        String strategy,
        int capacity,
        int refillPerSecond,
        int windowSeconds) {

    public RateLimitProperties {
        if (strategy == null || strategy.isBlank()) {
            strategy = "token-bucket";
        }
        if (capacity <= 0) {
            capacity = 10;
        }
        if (refillPerSecond <= 0) {
            refillPerSecond = 5;
        }
        if (windowSeconds <= 0) {
            windowSeconds = 10;
        }
    }
}
