package com.example.orderservice.service;

import com.example.orderservice.outbox.OutboxEvent;
import com.example.orderservice.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void shouldRequeueDeadLetteredEvent() {

        UUID eventId = UUID.randomUUID();
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(eventId)
                .aggregateId(UUID.randomUUID())
                .topic("order-created")
                .eventType("OrderCreatedEvent")
                .payload("{}")
                .attemptCount(3)
                .deadLettered(true)
                .lastError("Kafka is unavailable")
                .nextAttemptAt(Instant.now().plusSeconds(60))
                .build();
        when(outboxEventRepository.findById(eventId))
                .thenReturn(Optional.of(outboxEvent));

        outboxService.requeueDeadLetteredEvent(eventId);

        assertFalse(outboxEvent.isDeadLettered());
        assertEquals(0, outboxEvent.getAttemptCount());
        assertNull(outboxEvent.getLastError());
        verify(outboxEventRepository).findById(eventId);
    }
}
