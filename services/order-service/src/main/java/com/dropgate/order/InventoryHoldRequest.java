package com.dropgate.order;

public record InventoryHoldRequest(String dropId, String accountId, int quantity) {
}
