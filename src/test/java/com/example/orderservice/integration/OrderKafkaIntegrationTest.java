package com.example.orderservice.integration;

import com.example.orderservice.config.AbstractPostgresContainerTest;
import com.example.orderservice.config.KafkaTestContainer;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderKafkaIntegrationTest extends KafkaTestContainer {

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {

    }

    @Test
    void shouldPublishOrderCreatedEventToKafka() {

    }

}