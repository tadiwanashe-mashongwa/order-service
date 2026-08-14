package com.example.orderservice.integration;

import com.example.orderservice.config.KafkaTestContainer;
import com.example.orderservice.client.ApiResponse;
import com.example.orderservice.client.CatalogueClient;
import com.example.orderservice.client.MoneyResponse;
import com.example.orderservice.client.PartResponse;
import com.example.orderservice.dto.CreateOrderItemRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateOrderResponse;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.event.OrderItemEvent;
import com.example.orderservice.outbox.OutboxEvent;
import com.example.orderservice.outbox.OutboxEventRepository;
import com.example.orderservice.outbox.OutboxRelay;
import com.example.orderservice.producer.OrderEventProducer;
import com.example.orderservice.service.OrderService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderKafkaIntegrationTest extends KafkaTestContainer {

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private CatalogueClient catalogueClient;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers().getFirst(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "order-kafka-integration-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        ));
        consumer.subscribe(List.of("order-created"));
        consumer.poll(Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void shouldPublishOrderCreatedEventToKafka() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                customerId,
                List.of(new OrderItemEvent(UUID.randomUUID(), 2))
        );

        orderEventProducer.publishOrderCreated(event);

        ConsumerRecords<String, String> records =
                consumer.poll(Duration.ofSeconds(10));

        assertTrue(records.count() > 0);

        ConsumerRecord<String, String> record = null;
        for (ConsumerRecord<String, String> message : records.records("order-created")) {
            if (message.key().equals(orderId.toString())) {
                record = message;
                break;
            }
        }

        assertNotNull(record);
        assertEquals(orderId.toString(), record.key());
        assertTrue(record.value().contains("\"orderId\":\"" + orderId + "\""));
        assertTrue(record.value().contains("\"customerId\":\"" + customerId + "\""));
    }

    @Test
    void shouldRelayCreatedOrderFromOutboxToKafka() {
        UUID partId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(new CreateOrderItemRequest(partId, 1))
        );
        when(catalogueClient.getPartById(partId)).thenReturn(
                new ApiResponse<>(
                        true,
                        "Success",
                        new PartResponse(
                                partId,
                                "Brake Pads",
                                new MoneyResponse(250, "USD")
                        ),
                        Instant.now()
                )
        );

        CreateOrderResponse order = orderService.createOrder(request);

        OutboxEvent pendingEvent = outboxEventRepository.findAll().getFirst();
        assertFalse(pendingEvent.isPublished());

        outboxRelay.publishPendingEvents();

        ConsumerRecords<String, String> records =
                consumer.poll(Duration.ofSeconds(10));
        boolean receivedOrderEvent = false;
        for (ConsumerRecord<String, String> record :
                records.records("order-created")) {
            if (record.key().equals(order.orderId().toString())) {
                receivedOrderEvent = true;
                break;
            }
        }
        assertTrue(receivedOrderEvent);

        OutboxEvent outboxEvent = outboxEventRepository.findAll().getFirst();
        assertTrue(outboxEvent.isPublished());
    }
}
