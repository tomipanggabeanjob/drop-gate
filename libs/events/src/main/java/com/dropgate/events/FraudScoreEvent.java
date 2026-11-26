package com.dropgate.events;

public record FraudScoreEvent(
        String fingerprintId,
        String source,
        double score,
        long timestamp,
        int version
) {}
