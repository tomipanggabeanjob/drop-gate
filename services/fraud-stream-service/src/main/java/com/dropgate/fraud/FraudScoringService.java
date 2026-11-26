package com.dropgate.fraud;

import com.dropgate.events.ClickEvent;
import com.dropgate.events.FraudScoreEvent;
import org.springframework.stereotype.Service;

@Service
public class FraudScoringService {

    public FraudScoreEvent score(ClickEvent event) {
        return new FraudScoreEvent(
                event.fingerprintId(),
                event.action(),
                calculateRiskScore(event.action()),
                System.currentTimeMillis(),
                1);
    }

    private double calculateRiskScore(String action) {
        return switch (action) {
            case "queue_join" -> 82.0;
            case "checkout" -> 91.0;
            default -> 24.0;
        };
    }
}
