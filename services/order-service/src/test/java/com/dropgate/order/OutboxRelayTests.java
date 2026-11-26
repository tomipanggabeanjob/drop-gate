package com.dropgate.order;

import com.dropgate.events.OrderEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRelayTests {

    @Test
    void relayPublishesAndMarksPendingEvents() {
        OutboxRepository outboxRepository = mock(OutboxRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

        OutboxEvent event = new OutboxEvent(
                "order",
                "ord-1",
                "order-created",
                new OrderEvent("evt-1", "ord-1", "drop-1", "acct-1", "hold-1", "PAID", 4000L, "idempotency-1", System.currentTimeMillis(), 1)
        );
        when(outboxRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));

        OutboxRelay relay = new OutboxRelay(outboxRepository, kafkaTemplate);
        relay.flushPending();

        verify(kafkaTemplate).send(eq("order-events"), eq("evt-1"), argThat(payload ->
                payload instanceof OrderEvent orderEvent && orderEvent.orderId().equals("ord-1")));
        verify(outboxRepository).save(argThat(saved -> saved.getPublishedAt() != null));
    }
}
