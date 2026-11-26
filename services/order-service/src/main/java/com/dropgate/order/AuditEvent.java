package com.dropgate.order;

import java.time.Instant;

public record AuditEvent(
        String beanName,
        String methodName,
        String argumentsSummary,
        String user,
        String outcome,
        long durationMs,
        Instant timestamp) {
}
