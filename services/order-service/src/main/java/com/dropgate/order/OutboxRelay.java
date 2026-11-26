package com.dropgate.order;

import com.dropgate.events.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutboxRelay(OutboxRepository outboxRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5_000)
    public void relayPending() {
        flushPending();
    }

    public void flushPending() {
        List<OutboxEvent> pending = outboxRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                Object payload = switch (event.getEventType()) {
                    case "order-created" -> objectMapper.readValue(event.getPayload(), OrderEvent.class);
                    default -> objectMapper.readValue(event.getPayload(), Object.class);
                };
                String key = payload instanceof OrderEvent oe ? oe.eventId() : event.getAggregateId();
                kafkaTemplate.send(resolveTopic(event), key, payload);
                event.markPublished();
                outboxRepository.save(event);
            } catch (Exception ex) {
                // fail-safe: keep the event for the next relay cycle
                log.warn("Failed to relay outbox event {}", event.getId(), ex);
            }
        }
    }

    private String resolveTopic(OutboxEvent event) {
        return switch (event.getEventType()) {
            case "order-created" -> "order-events";
            case "inventory-changed" -> "inventory-changes";
            default -> "audit-events";
        };
    }
}
