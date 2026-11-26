package com.dropgate.order;

public record InventoryHoldResponse(String dropId, String accountId, int quantity, int remaining) {
}
