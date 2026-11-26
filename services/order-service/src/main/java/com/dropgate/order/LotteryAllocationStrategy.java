package com.dropgate.order;

import org.springframework.stereotype.Component;

@Component("lottery")
public class LotteryAllocationStrategy implements AllocationStrategy {
    @Override
    public String name() {
        return "lottery";
    }

    @Override
    public AllocationDecision decide(AllocationRequest request) {
        if (request.requestedQuantity() <= 0) {
            return AllocationDecision.rejected(name(), "Quantity must be positive");
        }
        if (request.availableStock() < request.requestedQuantity()) {
            return AllocationDecision.rejected(name(), "Insufficient stock");
        }
        return AllocationDecision.approved(name(), request.requestedQuantity());
    }
}
