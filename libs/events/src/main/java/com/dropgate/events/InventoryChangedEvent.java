package com.dropgate.events;

public record InventoryChangedEvent(
        String dropId,
        String reason,
        int delta,
        int remainingStock,
        long timestamp,
        int version
) {}
