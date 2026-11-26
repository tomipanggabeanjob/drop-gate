package com.dropgate.events;

public record OrderEvent(
        String eventId,
        String orderId,
        String dropId,
        String accountId,
        String holdId,
        String status,
        long totalCents,
        String idempotencyKey,
        long timestamp,
        int version
) {}
