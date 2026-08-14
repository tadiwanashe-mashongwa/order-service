package com.example.orderservice.outbox;

import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.event.OrderItemEvent;
import com.example.orderservice.producer.OrderEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                List.of(new OrderItemEvent(UUID.randomUUID(), 1))
        );
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(orderId)
                .topic("order-created")
                .eventType(OrderCreatedEvent.class.getSimpleName())
                .payload("{...}")
                .build();

        when(outboxEventRepository
                .findTop100ByPublishedFalseOrderByCreatedAtAsc())
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
}
