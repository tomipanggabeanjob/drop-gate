package com.dropgate.order;

import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final InventoryPort inventoryPort;

    public InventoryService(InventoryPort inventoryPort) {
        this.inventoryPort = inventoryPort;
    }

    public InventorySnapshot getInventory(String dropId) {
        return inventoryPort.getInventory(dropId);
    }

    public InventoryHoldResponse placeHold(InventoryHoldRequest request) {
        return inventoryPort.placeHold(request);
    }
}
