package com.example.orderservice.event;

import com.example.orderservice.entity.OrderStatus;

import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        UUID customerId,
        OrderStatus previousStatus,
        OrderStatus status
) {
}
