package com.dropgate.events;

public record ClickEvent(
        String eventId,
        String fingerprintId,
        String dropId,
        String accountId,
        String action,
        String userAgentHash,
        String ipHash,
        long timestamp,
        int version
) {}
