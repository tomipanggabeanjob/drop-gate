package com.dropgate.order;

public interface AuditEventSink {
    void publish(AuditEvent event);
}
