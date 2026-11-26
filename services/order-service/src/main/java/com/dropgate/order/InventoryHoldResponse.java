package com.dropgate.order;

public record InventoryHoldResponse(String holdId, String dropId, String accountId, int quantity, int remaining) {
}
