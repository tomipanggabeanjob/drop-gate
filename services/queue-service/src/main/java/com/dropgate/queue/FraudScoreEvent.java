package com.dropgate.queue;

public record FraudScoreEvent(
        String fingerprintId,
        double score,
        double threshold,
        long timestamp) {
}
