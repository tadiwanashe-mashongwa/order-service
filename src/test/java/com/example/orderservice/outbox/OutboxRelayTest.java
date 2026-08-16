package com.example.orderservice.outbox;

import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.event.OrderItemEvent;
import com.example.orderservice.event.OrderStatusChangedEvent;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.producer.OrderEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxRelay outboxRelay;

    @Test
    void shouldPublishPendingOrderCreatedEventAndMarkItPublished()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                UUID.randomUUID(),
                java.math.BigDecimal.TEN,
                List.of(new OrderItemEvent(UUID.randomUUID(), 1))
        );
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(orderId)
                .topic("order-created")
                .eventType(OrderCreatedEvent.class.getSimpleName())
                .payload("{...}")
                .build();

        when(outboxEventRepository
                .findTop100ByPublishedFalseAndDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        any(Instant.class)
                ))
                .thenReturn(List.of(outboxEvent));
        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                OrderCreatedEvent.class
        )).thenReturn(event);
        when(orderEventProducer.publishOrderCreated(event))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxRelay.publishPendingEvents();

        verify(orderEventProducer).publishOrderCreated(event);
        assertTrue(outboxEvent.isPublished());
    }

    @Test
    void shouldRecordFailureWhenKafkaPublishingFails()
            throws Exception {

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                java.math.BigDecimal.TEN,
                List.of()
        );
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(event.orderId())
                .topic("order-created")
                .eventType(OrderCreatedEvent.class.getSimpleName())
                .payload("{...}")
                .build();

        when(outboxEventRepository
                .findTop100ByPublishedFalseAndDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        any(Instant.class)
                ))
                .thenReturn(List.of(outboxEvent));
        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                OrderCreatedEvent.class
        )).thenReturn(event);
        when(orderEventProducer.publishOrderCreated(event))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka is unavailable")
                ));

        outboxRelay.publishPendingEvents();

        assertFalse(outboxEvent.isPublished());
        assertTrue(outboxEvent.getAttemptCount() == 1);
        assertTrue(outboxEvent.getLastError().contains("Kafka is unavailable"));
        assertTrue(outboxEvent.getNextAttemptAt().isAfter(Instant.now()));
    }

    @Test
    void shouldDeadLetterEventAfterThirdKafkaPublishingFailure()
            throws Exception {

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                java.math.BigDecimal.TEN,
                List.of()
        );
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(event.orderId())
                .topic("order-created")
                .eventType(OrderCreatedEvent.class.getSimpleName())
                .payload("{...}")
                .attemptCount(2)
                .build();

        when(outboxEventRepository
                .findTop100ByPublishedFalseAndDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        any(Instant.class)
                ))
                .thenReturn(List.of(outboxEvent));
        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                OrderCreatedEvent.class
        )).thenReturn(event);
        when(orderEventProducer.publishOrderCreated(event))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka is unavailable")
                ));

        outboxRelay.publishPendingEvents();

        assertFalse(outboxEvent.isPublished());
        assertTrue(outboxEvent.isDeadLettered());
        assertTrue(outboxEvent.getAttemptCount() == 3);
    }

    @Test
    void shouldPublishPendingOrderStatusChangedEventAndMarkItPublished()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                orderId,
                UUID.randomUUID(),
                OrderStatus.PENDING,
                OrderStatus.STOCK_RESERVED
        );
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(orderId)
                .topic("order-status-changed")
                .eventType(OrderStatusChangedEvent.class.getSimpleName())
                .payload("{...}")
                .build();

        when(outboxEventRepository
                .findTop100ByPublishedFalseAndDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        any(Instant.class)
                ))
                .thenReturn(List.of(outboxEvent));
        when(objectMapper.readValue(
                outboxEvent.getPayload(),
                OrderStatusChangedEvent.class
        )).thenReturn(event);
        when(orderEventProducer.publishOrderStatusChanged(event))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxRelay.publishPendingEvents();

        verify(orderEventProducer).publishOrderStatusChanged(event);
        assertTrue(outboxEvent.isPublished());
    }
}
