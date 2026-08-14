package com.example.orderservice.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxEventTest {

    @Test
    void shouldDoubleTheRetryDelayForEachFailure() {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(UUID.randomUUID())
                .topic("order-created")
                .eventType("OrderCreatedEvent")
                .payload("{}")
                .attemptCount(1)
                .build();
        Instant beforeFailure = Instant.now();

        outboxEvent.recordFailure(new IllegalStateException("Kafka is unavailable"));

        assertEquals(2, outboxEvent.getAttemptCount());
        assertTrue(!outboxEvent.getNextAttemptAt()
                .isBefore(beforeFailure.plusSeconds(2)));
        assertTrue(!outboxEvent.getNextAttemptAt()
                .isAfter(Instant.now().plusSeconds(2)));
    }
}
