package com.example.orderservice.producer;

import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.event.OrderStatusChangedEvent;
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

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>>
    publishOrderCreated(OrderCreatedEvent event) {

        log.info("Publishing OrderCreatedEvent for order {}", event.orderId());

        CompletableFuture<SendResult<String, Object>> result = kafkaTemplate.send(
                "order-created",
                event.orderId().toString(),
                event
        );

        return result;
    }

    public CompletableFuture<SendResult<String, Object>>
    publishOrderStatusChanged(OrderStatusChangedEvent event) {

        log.info("Publishing OrderStatusChangedEvent for order {}", event.orderId());

        return kafkaTemplate.send(
                "order-status-changed",
                event.orderId().toString(),
                event
        );
    }
}
