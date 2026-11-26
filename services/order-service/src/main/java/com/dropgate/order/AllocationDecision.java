package com.dropgate.order;

public record AllocationDecision(String strategyName, boolean approved, int allocatedQuantity, String reason) {
    public static AllocationDecision approved(String strategyName, int allocatedQuantity) {
        return new AllocationDecision(strategyName, true, allocatedQuantity, "Approved");
    }

    public static AllocationDecision rejected(String strategyName, String reason) {
        return new AllocationDecision(strategyName, false, 0, reason);
    }
}
