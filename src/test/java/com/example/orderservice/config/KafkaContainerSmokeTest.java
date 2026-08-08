package com.example.orderservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class KafkaContainerSmokeTest extends KafkaTestContainer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldStartKafkaContainer() {

        assertNotNull(kafkaTemplate);
        assertNotNull(kafka.getBootstrapServers());

    }

}