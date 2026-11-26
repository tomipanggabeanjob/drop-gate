package com.dropgate.order;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class AllocationService {
    private final Map<String, AllocationStrategy> strategies;

    public AllocationService(Map<String, AllocationStrategy> strategies) {
        this.strategies = strategies;
    }

    public AllocationDecision decide(String strategyName, AllocationRequest request) {
        String normalized = strategyName == null ? "fifo" : strategyName.toLowerCase(Locale.ROOT);
        AllocationStrategy strategy = strategies.getOrDefault(normalized, strategies.get("fifo"));
        if (strategy == null) {
            return AllocationDecision.rejected("fifo", "No allocation strategy registered");
        }
        return strategy.decide(request);
    }
}
