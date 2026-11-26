package com.dropgate.order;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfiguration {

    @Bean
    public AuditEventSink auditEventSink() {
        return event -> {
            // Intentionally lightweight: the sink is the extension point for downstream audit persistence.
            System.out.println("AUDIT_EVENT " + event);
        };
    }

    @Bean
    public AuditedBeanPostProcessor auditedBeanPostProcessor(AuditEventSink auditEventSink) {
        return new AuditedBeanPostProcessor(auditEventSink, "system");
    }
}
