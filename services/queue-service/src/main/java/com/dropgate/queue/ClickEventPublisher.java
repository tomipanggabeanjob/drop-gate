package com.dropgate.queue;

import com.dropgate.events.ClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.UUID;

@Component
public class ClickEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClickEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ClickEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String dropId, String accountId, String fingerprint, String action) {
        String fingerprintId = fingerprint == null || fingerprint.isBlank() ? accountId : fingerprint;
        ClickEvent event = new ClickEvent(
                UUID.randomUUID().toString(),
                fingerprintId,
                dropId,
                accountId,
                action,
                null,
                null,
                System.currentTimeMillis(),
                1);
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send("click-events",
                        fingerprintId,
                        event);
            } catch (RuntimeException ex) {
                // Queue admission remains available when the event stream is unavailable.
                log.warn("Failed to publish click event for drop {}", dropId, ex);
            }
        });
    }
}
