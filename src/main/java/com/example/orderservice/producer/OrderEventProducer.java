package com.example.orderservice.producer;

import com.example.orderservice.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {

        log.info("Publishing OrderCreatedEvent for order {}", event.orderId());

        kafkaTemplate.send(
                "order-created",
                event.orderId().toString(),
                event
        );

        log.info("OrderCreatedEvent published successfully for order {}", event.orderId());
    }
}