package com.dropgate.queue;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FraudScoreConsumer {

    private static final long FLAG_TTL_MS = 30_000L;

    private final RateLimitService rateLimitService;

    public FraudScoreConsumer(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @KafkaListener(topics = "fraud-scores", groupId = "${spring.kafka.consumer.group-id:queue-service}")
    public void handle(Map<String, Object> event) {
        if (event == null || event.isEmpty()) {
            return;
        }

        Object fingerprint = event.get("fingerprintId");
        Object scoreObj = event.get("score");
        Object thresholdObj = event.getOrDefault("threshold", 75.0);
        if (fingerprint == null || scoreObj == null) {
            return;
        }

        String fingerprintId = String.valueOf(fingerprint);
        double score = Double.parseDouble(String.valueOf(scoreObj));
        double threshold = Double.parseDouble(String.valueOf(thresholdObj));

        if (score > threshold) {
            rateLimitService.flagFingerprint(fingerprintId, FLAG_TTL_MS);
        }
    }
}
