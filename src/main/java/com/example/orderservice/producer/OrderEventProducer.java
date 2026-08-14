package com.example.orderservice.producer;

import com.example.orderservice.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public CompletableFuture<SendResult<String, OrderCreatedEvent>>
    publishOrderCreated(OrderCreatedEvent event) {

        log.info("Publishing OrderCreatedEvent for order {}", event.orderId());

        CompletableFuture<SendResult<String, OrderCreatedEvent>> result = kafkaTemplate.send(
                "order-created",
                event.orderId().toString(),
                event
        );

        return result;
    }
}
