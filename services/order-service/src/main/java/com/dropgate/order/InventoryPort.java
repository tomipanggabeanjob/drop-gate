package com.dropgate.order;

public interface InventoryPort {
    InventorySnapshot getInventory(String dropId);

    InventoryHoldResponse placeHold(InventoryHoldRequest request);

    void releaseHold(String dropId, String holdId, int quantity);
}
