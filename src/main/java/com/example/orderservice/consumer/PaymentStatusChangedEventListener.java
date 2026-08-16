package com.example.orderservice.consumer;

import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.PaymentStatusChangedEvent;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentStatusChangedEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PaymentStatusChangedEventListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-status-changed", groupId = "order-service")
    public void handlePaymentStatusChanged(String payload) {
        try {
            PaymentStatusChangedEvent event = objectMapper.readValue(payload, PaymentStatusChangedEvent.class);
            if ("SUCCESS".equals(event.status())) {
                orderService.transitionOrderStatus(event.orderId(), OrderStatus.PAID);
            } else if ("FAILED".equals(event.status())) {
                orderService.transitionOrderStatus(event.orderId(), OrderStatus.PAYMENT_FAILED);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid payment-status-changed event", exception);
        }
    }
}
