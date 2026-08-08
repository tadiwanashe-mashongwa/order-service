package com.example.orderservice.integration;

import com.example.orderservice.config.KafkaTestContainer;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.producer.OrderEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest
class OrderEventProducerIntegrationTest extends KafkaTestContainer {

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Test
    void shouldPublishOrderCreatedEvent() {

    }

}