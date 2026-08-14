package com.example.orderservice.outbox;

import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.producer.OrderEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEvent outboxEvent :
                outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc()) {
            publish(outboxEvent);
            outboxEvent.markPublished();
        }
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(
                    outboxEvent.getPayload(),
                    OrderCreatedEvent.class
            );
            orderEventProducer.publishOrderCreated(event).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Failed to publish outbox event " + outboxEvent.getId(),
                    e
            );
        } catch (JsonProcessingException | ExecutionException e) {
            throw new IllegalStateException(
                    "Failed to publish outbox event " + outboxEvent.getId(),
                    e
            );
        }
    }
}
