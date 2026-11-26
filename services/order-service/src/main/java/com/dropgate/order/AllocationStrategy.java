package com.dropgate.order;

public interface AllocationStrategy {
    String name();

    AllocationDecision decide(AllocationRequest request);
}
