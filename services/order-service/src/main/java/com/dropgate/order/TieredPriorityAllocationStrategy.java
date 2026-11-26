package com.dropgate.order;

import org.springframework.stereotype.Component;

@Component("tiered")
public class TieredPriorityAllocationStrategy implements AllocationStrategy {
    @Override
    public String name() {
        return "tiered";
    }

    @Override
    public AllocationDecision decide(AllocationRequest request) {
        if (request.requestedQuantity() <= 0) {
            return AllocationDecision.rejected(name(), "Quantity must be positive");
        }
        if (request.availableStock() < request.requestedQuantity()) {
            return AllocationDecision.rejected(name(), "Insufficient stock");
        }
        if (request.accountId() != null && request.accountId().toLowerCase().startsWith("vip")) {
            return AllocationDecision.approved(name(), request.requestedQuantity());
        }
        return AllocationDecision.approved(name(), request.requestedQuantity());
    }
}
