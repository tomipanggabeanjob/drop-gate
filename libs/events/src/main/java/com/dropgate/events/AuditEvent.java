package com.dropgate.events;

public record AuditEvent(
        String entityId,
        String entityType,
        String action,
        String actor,
        String outcome,
        long timestamp,
        int version
) {}
