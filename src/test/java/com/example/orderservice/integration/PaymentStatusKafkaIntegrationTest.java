package com.example.orderservice.integration;

import com.example.orderservice.config.KafkaTestContainer;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.PaymentStatusChangedEvent;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentStatusKafkaIntegrationTest extends KafkaTestContainer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Test
    void shouldMarkPaymentPendingOrderAsPaidFromKafkaPaymentSuccessEvent() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .customerId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build());
        orderService.transitionOrderStatus(order.getId(), OrderStatus.STOCK_RESERVED);
        orderService.transitionOrderStatus(order.getId(), OrderStatus.PAYMENT_PENDING);
        ContainerTestUtils.waitForAssignment(
                kafkaListenerEndpointRegistry.getListenerContainers().iterator().next(),
                1
        );

        kafkaTemplate.send(
                "payment-status-changed",
                UUID.randomUUID().toString(),
                new PaymentStatusChangedEvent(UUID.randomUUID(), order.getId(), "SUCCESS")
        ).get();

        assertThat(waitForStatus(order.getId())).isEqualTo(OrderStatus.PAID);
    }

    private OrderStatus waitForStatus(UUID orderId) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            OrderStatus status = orderRepository.findById(orderId).orElseThrow().getStatus();
            if (status == OrderStatus.PAID) {
                return status;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for order " + orderId + " to be PAID");
    }
}
