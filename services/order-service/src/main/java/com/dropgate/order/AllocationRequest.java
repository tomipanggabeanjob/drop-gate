package com.dropgate.order;

public record AllocationRequest(String dropId, String accountId, int requestedQuantity, int availableStock, String strategyName) {
}
